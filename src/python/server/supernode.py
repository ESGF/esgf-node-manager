import os, json


from threading import Thread

from nodemgr.nodemgr.healthcheck import RunningCheck, BasicSender
#from httplib import HTTPConnection, HTTPException

import requests

from nodemgr.nodemgr.simplequeue import write_task

from nodemgr.nodemgr.site_profile import gen_reg_xml, REG_FN

from nodemgr.nodemgr.settings import PROTO


import logging



#TODO - health check should include a timestamp of the current properties in the nodes possession 

def quick_check():

    #conn = HTTPConnection("localhost", PORT, timeout=1)
    URLpath = "/esgf-nm"
    
    try:
   #     conn.request("GET", URLpath)
   #     resp = conn.getresponse()
        if resp.status == 200:
            return True
    except Exception as e:
        pass

    return False




class NMapSender(BasicSender):

    def __init__(self,nmap, nn, ts=0):
        super(NMapSender, self).__init__()
        self.nodemap = nmap.nodemap
        self.target = nn
        self.ts = ts
        self.fromnode = nmap.myid
        self.logger = logging.getLogger("esgf_nodemanager")

    def run(self):

#        print self.target, PORT 

        tstr = ""

        if self.ts>0:
            tstr = "&timestamp=" + str(self.ts)
        

        try:

            resp = requests.post(self.mkurl("/esgf-nm/api?action=node_map_update" + tstr + "&from=" + self.fromnode) , data=json.dumps(self.nodemap), verify='/etc/grid-security/certificates/' )

            if resp.status_code == 500:
                self.logger.error(resp.text)

        except Exception as e:
            print "Connection problem: ",e, self.target


class SNInitSender(BasicSender):

    def __init__(self, nn, ts, nmap):
        super(SNInitSender, self).__init__()

        self.target = nn
        self.ts = ts
        self.fromnode = nmap.myid
        self.logger = logging.getLogger("esgf_nodemanager")

    def run(self):

#        print self.target, PORT 

        tstr = "&timestamp=" + str(self.ts)

        try: 
            resp = requests.get(self.mkurl("/esgf-nm/api?action=sn_init" + tstr + "&from=" + self.fromnode), verify='/etc/grid-security/certificates/' )
            
            if resp.status_code == 500:
                self.logger.error(resp.text)


        except Exception as e:
            print "Connection problem: ",e, self.target


class NMRepoSender(BasicSender):

    def __init__(self, nn, task_d, nmap, ts):
        super(NMRepoSender, self).__init__()

        self.target = nn
        self.task_d = task_d
        self.fromnode = nmap.myid
        self.ts = ts
        self.logger = logging.getLogger("esgf_nodemanager")    

    def get_url_str(self):
        
        parts = ["application", "project", "name", "send"]

        arr = []
        for n in parts:
            arr.append("&")
            arr.append(n)
            arr.append("=")
            arr.append(self.task_d[n])
        
        return ''.join(arr)
        

    def run(self):


#        print self.target, PORT 
 

        tstr = "&timestamp=" + str(self.ts)

        try:
            resp = requests.post(self.mkurl("/esgf-nm/api?action=nm_repo_update" + tstr + "&from=" + self.fromnode + get_url_str()), data=json.dumps(self.task_d["update"]) , verify='/etc/grid-security/certificates/')
            if resp.status_code == 500:
                print "500 error - Logged"
                self.logger.error(resp.text)


        except Exception as e:
            print "Connection problem: ",e, self.target


localhostname = os.uname()[1]

def node_redist(nm_inst, sn_id):


#    pdb.set_trace()

    MAX_PER_SN = 3

# Find entry for down node
    
    free_slots = []

    x = None

    for n in nm_inst["membernodes"]:

        if n["supernode"] == sn_id:


            x = n
        else:
            if len(n["members"]) < MAX_PER_SN:
                free_slots.append([n, MAX_PER_SN - len(n["members"])])
            


    if x is None:
        print "supernode node found"
        return True
    else:
        x["status"] = "reassigned"  
        
    idx = 0

    mem = x["members"]

    if len(mem) == 0:
        print "No nodes to reassign"
        return True

    dup_ids = []

    for node_obj in mem:

        remove = False
        for comp_obj in nm_inst["membernodes"]:

            if comp_obj["supernode"] == sn_id:
                continue
            for comp_mn in comp_obj["members"]:
                if not "temp_assign" in comp_mn:
                    comp_mn["temp_assign"] = False
                if node_obj["id"] == comp_mn["id"] and comp_mn["temp_assign"]:
                    print "Found duplicate", node_obj
                    remove = True
                    break
            if remove:
                break
        if remove:
            dup_ids.append(node_obj["id"])

    summ = 0
    for z in free_slots:
        summ+=z[1]

# need to promote a member node if nothing available
    if summ < len(mem):
        return False
    
    if idx >= len(mem):
        return True
        
    for z in free_slots:

        for i in range(z[1]):

            if not mem[idx]["id"] in dup_ids:
                cl = mem[idx].copy()
            
                node_id = cl["id"]
            
                cl["temp_assign"] = True
                cl["prev_owner"] = sn_id

                print z

                z[0]["members"].append(cl)
            else:
                print mem[idx]["id"], "was already added to an alternate nodes list"
            idx=idx+1
            if idx == len(mem):
                return True


def node_return(nm_inst, sn_id):

    print "returnin node to org position"

    for n in nm_inst["membernodes"]:


        if n["supernode"] == sn_id:

            if n["status"] == "OK":
                return
            

            n["status"] = "OK"
        else:
            for x in n["members"]:
                
                print x
                if "temp_assign" in x and x["temp_assign"] and x["prev_owner"] == sn_id:
                    print " removing the entry"
                    tmp = n["members"]
                    tmp.remove(x)
                    n["members"] = tmp
            print "New super entry = ", n

def supernode_check(nodemap_instance):

    tarr = []

    for n in nodemap_instance.get_supernode_list():


        if n != localhostname:
            t = RunningCheck(n, True)
            t.start()
            tarr.append(t)


    report_dict = {}

    report_dict["action"] = "health_check_report"
    report_dict["from"] = localhostname

    for tt in tarr:

        tt.join()

        report_dict[tt.nodename] = tt.eltime
    
    
    write_task(json.dumps(report_dict))
#    health_check_report(report_dict, nodemap_instance)


def check_properties(nodemap_instance):


    tmp_props = {}


    for snode_obj, mnode in zip(nodemap_instance.nodemap["supernodes"], nodemap_instance.nodemap["membernodes"]):
        

        # for now don't request things we have. TODO: update if the
        # timestamp is too stale (should be more frequent than the
        # health checks).
        target = snode_obj["hostname"]

        # If we don't have that supernode's record, fetch it
        fetch_cond = not target in nodemap_instance.prop_store;
        
        if not fetch_cond:
            for mb in mnode["members"]:
                mbtarg = mb["hostname"]
                if not mbtarg in nodemap_instance.prop_store:
                    fetch_cond = True
                    break
        
        if  fetch_cond and target != localhostname and snode_obj["health"] == "good":
            
            print "retrieving", target, "properties"

            resp = None

            err = ""
            
            snder = BasicSender()
            snder.target = target 

            try:
                resp = requests.get(snder.mkurl("/esgf-nm/node-props.json"), verify='/etc/grid-security/certificates/')


            except Exception as e:
                print "Connection problem: ",e, self.target
                err = str(e)


            if not resp is None and resp.status_code == 200:
                dat = resp.text

#                print dat
                if dat == "NO_FILE":
                    print "No File found: ", target
                    continue
                

                obj = []
                try:
                    obj = json.loads(dat)
                except:
                    print "JSON error: " , target
                    continue

                # TODO: Are we producing duplicate entries?
                for kvp in obj:

                    if not kvp == "ts_all": 
                        val = obj[kvp]
                        tmp_props[kvp] = val

            else:
                # TODO: log these sorts of errors
                print "An Error has occurred"
                lg = logging.getLogger("esgf_nodemanager")

                if not resp is None:
                    lg.error(resp.text)
                else:
                    lg.error("Connection Problem:" + err)




    for n in nodemap_instance.prop_store:
        val = nodemap_instance.prop_store[n]

        if not n in tmp_props:
            
            tmp_props[n] = val
    
    #TODO update the prop store with more recent info

    out_xml = gen_reg_xml(tmp_props)
        
    f = open(REG_FN, 'w')
    f.write(out_xml)
    f.close()



def send_map_to_others(members, nmap, ts=0):
    
    nodes = []

    if members:
        nodes = nmap.get_member_nodes()
    else:
        nodes = nmap.get_supernode_list()


    if (nodes is None) or len(nodes) == 0:
        return

    tarr = []

    for nn in nodes:
        if nn != nmap.myname:

            nms = NMapSender(nmap, nn, ts)

            nms.start()
            tarr.append(nms)

    for t in tarr:
        t.join()


def send_repo_upd_to_others(task_d, nmap):

    # get supernodes -  omit project for now
    nodes = nmap.get_supernode_list()


    clond = task_d.copy()

    clond["send"] = False

    # but get the project here
    
   
    if (nodes is None) or len(nodes) == 0:
        return 

    tarr = []

    for nn in nodes:
        if nn != nmap.myname:

            nms = NMRepoSender(task_d, nn, ts)

            nms.start()
            tarr.append(nms)

    nodes = nmap.get_member_nodes(task_d["project"])    

    for nn in nodes:

        nms = NMRepoSender(task_d, nn, ts)

        nms.start()
        tarr.append(nms)


    for t in tarr:
        t.join()
        return

    


def supernode_init(nmap, ts):

    nodes = nmap.get_supernode_list()

    tarr = []

    for nn in nodes:
        if nn != nmap.myname:

            inits = SNInitSender(nn, ts, nmap)

            inits.start()
            tarr.append(inits)

    for t in tarr:
        t.join()
    

def my_turn(time_delta,  sn_id, total_sn, total_period):



    mod_delta = time_delta % ( total_sn * total_period)

#    print "turn check:", mod_delta, sn_id, total_period
    
    if ( mod_delta  >= (sn_id -1) * total_period and mod_delta <  sn_id * total_period): 
        return True
    else:
        return False
        
def calc_time(curr, start, q, sl):

    return int((( curr - start) / sl ) % q)


def member_node_check(nmap):

    nodes = nmap.get_member_nodes()

    if (nodes is None) or len(nodes) == 0:
        return


    tarr = []

    for nn in nodes:

        r= RunningCheck(nn, False, False)
        tarr.append(r)
        r.start()

#        print str(r), " started"
    
    for t in tarr:

        t.join()
        
 #       print str(t), "joined"
        status = "good"

        if t.eltime < 0:
            status = "unreachable"
   #     else:
    #        print "eltime " , eltime 
# For now we don't care about time to reach a member node - potential
# future optimization
        
        if (nmap.update_membernode_status(t.nodename, status)):
            send_map_to_others(False, nmap)
            send_map_to_others(True, nmap)

        

def links_check(nmap):

    print "Links Check"

#    pdb.set_trace()

    changed = False
    
    snodes = nmap.nodemap["supernodes"]

    new_down = []

    new_back_up = []
    
    for i in range(nmap.nodemap["total_supernodes"]):
        
        
        
        nid = str(i+1)

        if (nid == nmap.myid):
            snodes[i]["health"] = "good"
            continue

        print "check on", i

        for v in nmap.nodemap["links"]:
            
            down = True

            if (nid == v["from"] or nid == v["to"]) and (v["status"] != "down"):
                down = False
#                print " found an up link"
                break

        
        
        if down and snodes[i]["health"] != "unreachable":
            snodes[i]["health"] = "unreachable"
            changed = True

            new_down.append(snodes[i]["id"])

            print "  changed bad"
        elif (not down) and (snodes[i]["health"] == "unreachable"):
            snodes[i]["health"] = "good"
            changed = True
            print "  change to good - reachable"
            new_back_up.append(snodes[i]["id"])
        elif (not down) and snodes[i]["health"] == "new":
            snodes[i]["health"] = "good"
            changed = True

        

    for n in snodes:

        hn = n["hostname"]

        if (not n["id"] in new_down) and hn in nmap.prop_store:

            hn_dict = nmap.prop_store[hn]

            if "status" in hn_dict:
                status = hn_dict["status"]
        
                if status == "LAPSED":

                    if n["health"] == "good":
                        new_down.append(n["id"])
                        changed = True
                    n["health"] = "bad"


                elif status == "ISSUE":  # we will need to define these
                    n["health"] = "unhealthy"
                    changed = True
                else:
                    print n["health"], status
                    
                    if n["health"] in ["bad", "unhealthy"]:
                        print "   change of health to good"
                        new_back_up.append(n["id"])
                        changed = True
                        n["health"] = "good"

    if changed:
        nmap.dirty = True

        
        i =0

#        need_to promote 
        for sn_id in new_down:

            print "Supernode down and dealing with it", sn_id
            if (not node_redist(nmap.nodemap, sn_id)):

                for id2 in new_down[i:]:
                    promote_members(nmap, id2)
                break
            i = i + 1


        for sn_id in new_back_up:
            node_return(nmap.nodemap, sn_id)


        send_map_to_others(False, nmap)
        send_map_to_others(True, nmap)
            
                    
