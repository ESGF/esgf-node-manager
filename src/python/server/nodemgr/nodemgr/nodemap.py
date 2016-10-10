import json
import os


# this should be set by a global config
MAXREF = 20



PROPS_FN = '/esg/config/nm.properties'

from site_profile import ts_func

class NodeMap():
    
    def __init__(self):
        self.filename = ""
        self.readonly = False
        self.prop_store = {} 
        self.prop_dirty = False

    def set_ro(self):
        self.readonly = True

    def load_map(self,MAPFILE):

        if (self.filename != ""):
            print "Initial map already loaded"
            return
        
        f = open(MAPFILE)
        
        self.nodemap = json.loads(f.read())
        f.close()
        self.myname = os.uname()[1]

        self.snidx = {}
        myid = -1
        for n in self.nodemap["supernodes"]:
            
            hostname = n["hostname"]
            self.snidx[hostname] = n["id"]
            
            if hostname == self.myname:
                myid = n["id"]
        
        if myid == -1:
            print "Node not in supernode list.  run as a member node"
        #     exit (1)

        self.myid = myid
        self.dirty = False
        
        self.filename = MAPFILE
        
        

    def num_members(self,x):

        return len(x["members"])

    def get_supernode_list(self, project=""):

        if len(project) > 0:
            sns = self.nodemap("supernodes")
            arr = []
            for n in sns:

                if project in n["projects"]:
                    arr.append(n["hostname"])
            return arr
        else:

            return self.snidx.keys()

    def get_member_nodes(self, project=""):

        mns = self.nodemap["membernodes"]

        for n in mns:

            if n["supernode"] == self.myid:
                
                if "members" in n:
                    
                    
                    if len(project) > 0:
                        arr = []
                        for row in n["members"]:
                            if project  == row["project"]:  # move to multi membership
                                arr.append(row["hostname"])
                    else:
                        return [row["hostname"] for row in n["members"]]
                else:
                    return []

    def update_membernode_status(self, mn_hostname, status):

        update = False

        mns = self.nodemap["membernodes"]

        for n in mns:

            if n["supernode"] == self.myid:
                for i in n["members"]:
                    if i["hostname"] == mn_hostname and i["health"] != status:
                        i["health"] = status
                        self.dirty = True
                        update = True
        return update
        
                        

    def assign_node_fewest(self, node_name, project, standby=False):

        
        fewest = {}
        ref = self.nodemap["membernodes"]

        mincount = MAXREF + 1

        for entry in ref:
            
            found = False
        

            if ( "members" in entry):
                members = entry["members"]
                count = len(members)
                for mn in members:
                    if mn["hostname"] == node_name:
                        if mn["standby"] != standby:
                            mn["standby"] = standby
                            self.dirty = True
                        found = True
                        if int(entry["supernode"]) == self.myid:
                            return True
                        return False
                        
                if count < mincount:
                    mincount = count
                    fewest = entry
            else:
                newlist = []
                entry["members"] = newlist
                fewest = entry
                break


        fewest = min(ref, key=lambda x: len(x["members"]))


        mnode_count = int(self.nodemap["total_membernodes"])
        snode_count = int(self.nodemap["total_supernodes"])
        
        mnode_count = mnode_count + 1

        new_node = {}
        new_node["id"] = str(mnode_count + snode_count)
        new_node["hostname"] = node_name
        new_node["standby"] = standby
        new_node["project"] = project
        new_node["health"] = "unverified"
        fewest["members"].append(new_node)

        self.nodemap["total_membernodes"] = mnode_count

        self.dirty = True

        if fewest["supernode"] == self.myid:
            return True

        return False


    def assign_node(self, node_name, project, standby=False):

        newentry = {}

        ref = self.nodemap["membernodes"]

        max_count = MAXREF

        count =0

        myentry = None

        print "I am adding an entry and my id is ", self.myid

        for entry in ref:
            
            found = False
        


            if ( "members" in entry):

#                print "entry is this: ", str(entry)
                members = entry["members"]

                count = len(members)

                if int(entry["supernode"]) == int(self.myid):
                    
 #                   print "found the entry"

                    myentry = entry
                    if "max_count" in entry:
                        
                        max_count = entry["max_count"]


                for mn in members:
                    if mn["hostname"] == node_name:

                        return True
                        

            else:
                newlist = []
                entry["members"] = newlist
                if int(entry["supernode"]) == int(self.myid):
                    print "found the empty entry"
                    myentry = entry



#        fewest = min(ref, key=lambda x: len(x["members"]))


        if myentry is None:
            print "myentry not found"
            return self.assign_node_fewest(node_name, project, standby)

        if count == max_count:
            print "max count for supernode has been reached"
            return self.assign_node_fewest(node_name, project, standby)

        mnode_count = int(self.nodemap["total_membernodes"])
        snode_count = int(self.nodemap["total_supernodes"])
        
        mnode_count = mnode_count + 1

        new_node = {}
        new_node["id"] = str(mnode_count + snode_count)
        new_node["hostname"] = node_name
        new_node["standby"] = standby
        new_node["project"] = project
        new_node["health"] = "unverified"
        myentry["members"].append(new_node)

        self.nodemap["total_membernodes"] = mnode_count

        self.dirty = True


        return False



    def remove_member(self, hostname):

        for entry in self.nodemap["membernodes"]:

            if entry["supernode"] == self.myid:
                
                memlist = entry["members"]
                for member in memlist:

                    if member["hostname"] == hostname:

                        memlist.remove(member)
                        self.dirty = True
                        self.nodemap["total_membernodes"] = self.nodemap["total_membernodes"] - 1
                        
                        print "success in removing member node"

                        return True
                print "did not find the member node for a removal"

                return False
                     
        print "did not match the supernode for a removal"
        return False

         

    def write_back(self):
        if self.readonly:
            print "Warning. attempt to write read only structure"
            return

        # We only write if there are changes


        self.prop_ts = ts_func()
        outf = open(PROPS_FN, 'w') 
        self.prop_store["ts_all"] = self.prop_ts
        outs = json.dumps(self.prop_store, sort_keys=True, indent=4, separators=(',', ': '))
        outf.write(outs)
        outf.close()


        if self.dirty == False:
            return
        
        print ("updating the file")
        outs = json.dumps(self.nodemap, sort_keys=True, indent=4, separators=(',', ': '))

        f = open(self.filename, "w")
        f.write(outs)
        f.close()
        self.dirty = False

    def get_indv_node_status_json(self):

        tmparr = []

        for n in self.nodemap["supernodes"]:
            
            tmparr.append(n)
        for n in self.nodemap["membernodes"]:
            show = True
            
            if "status" in n and n["status"] == "reassigned":
                show = False

            if show:
                for x in n["members"]:

                    tmparr.append(x)

        STR = json.dumps(tmparr, indent=4, separators=(',', ': '))

        return STR
            
    def reload(self):
        f = open(self.filename)
        
        self.nodemap = json.loads(f.read())
        f.close()        
        
    def set_prop(self, k, v):
        
        if k in self.prop_store:
            self.prop_store[k].update(v)
        else:
            self.prop_store[k] = v
        self.prop_dirty = True


nodemap_instance = NodeMap()

def get_instance():
    return nodemap_instance


