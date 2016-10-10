import json, os, logging



lg = logging.getLogger("esgf_nodemanager")

from time import time
from nodemgr.nodemgr.simplequeue import get_next_task
from nodemgr.nodemgr.healthcheck import RunningCheck
hostname = os.uname()[1]

from supernode import send_map_to_others

from time_store import get_instance

# for now not used
#from user_api import put_file

def task_set_status(task_d, nmap):

    print "Got the status message:", task_d["status"]


def task_node_properties(task_d, nmap):


    ky = task_d["esgf.host"]
#    print "node properties task" + ky
    status = task_d["status"]

    nmap.set_prop(ky, task_d)

    if not ky in nmap.snidx:

        
        if (nmap.update_membernode_status(ky, status )):
            send_map_to_others(True, nmap)        
            send_map_to_others(False, nmap)
        




def task_health_check_fwd(task_d, nmap):

    fromnode = task_d["from"]

#    tarr = []
        
    checkarr = []
    first = True
# Refactor with manager.py block
    for n in nmap.get_supernode_list():
            
            
        if (n != hostname and  n != fromnode ):

            t = RunningCheck(n, False, first, checkarr , fromnode)
            t.start()
            first = False
            #               tarr.append(t)  
 #   for t in tarr:
 #       t.join()


def task_health_check_report(task_d, nmap):

    from_node = task_d["from"]

    from_id = nmap.snidx[from_node]

    edgelist = nmap.nodemap["links"]

    

    for n in task_d.keys():

        if n != "from" and n != "action":
            
            speed = float(task_d[n])

            if not n in nmap.snidx:
#                print n,"not found in list"
#                print "TODO: fetch new master list and broadcast"
                continue

            to_id = nmap.snidx[n]
            
            if (int (from_id) < int(to_id)): 

                for ee in edgelist:
                    if ee["from"] == from_id and ee["to"] == to_id:
                        if speed < 0:
                            ee["status"] = "down"
                        else:
                            ee["status"] = "up"
                        ee["speed"] = float(speed)
                        nmap.dirty = True
                        break
            elif (int (from_id) > int(to_id)):
                for ee in edgelist:
                    if ee["to"] == from_id and ee["from"] == to_id:
                        if speed < 0:
                            ee["status"] = "down"
                        else:
                            ee["status"] = "up"
                        ee["speed"] = float(speed)
                        nmap.dirty = True
                        break

def task_node_map_update(task_d, nmap):
    # TODO:  nodemap object updates should be signed

    new_map_str = task_d["update"]

    new_map_obj = None
    
    try:
        new_map_obj = json.loads(new_map_str)
    except:
        print "Illegal string (can't load json)"
        lg.error("BAD JSON" + new_map_str)
        return False
    
    new_ts = 1
    old_ts = 0
 
    if "create_timestamp" in new_map_obj:
        new_ts = int(new_map_obj["create_timestamp"])
 
    if "create_timestamp" in nmap.nodemap:
        old_ts = int(nmap.nodemap["create_timestamp"])

    old_sn_lst = nmap.snidx.keys()
    old_sn_lst.sort()

    new_sn_lst = []

    for n in new_map_obj["supernodes"]:
        new_sn_lst.append(n["hostname"])
        
    new_sn_lst.sort()

    # We only care about the timestamp if there's been a change in the
    #  supernodes for now we assume that a newer timestamp (generated
    #  on install) will beat out old because the install will have
    #  fetched the latest version of the mapfile.  Conceivably, a map
    #  with a newer timestamp could have been generated manually with
    #  an outdated list, but because this scenario seems unlikely, we
    #  hope to stick with this procedure for updating the nodemap.

    if new_ts >= old_ts or old_sn_lst == new_sn_lst:
        # merge object maps
        nmap.nodemap = new_map_obj
    else:
        print "New map is based on old format, not updating"

        return False

    nmap.dirty = True
    send_map_to_others(True, nmap)
    return True

def task_nm_repo_update(task_d, nmap):
    
    upd = task_d["update"]

    put_file(task_d["application"], task_d["project"], task_d["name"], upd)

    if task_d["send"]:
        
        send_repo_upd_to_others(task_d, nmap)


def task_add_member(task_d, nmap):

    ts = int(time())

    rc = nmap.assign_node(task_d["from"], task_d["project"], task_d["standby"] == "True")
    send_map_to_others(False, nmap)
    send_map_to_others(True, nmap)        



def task_remove_member(task_d, nmap):
    print "From", task_d["from"]
    if nmap.remove_member(task_d["from"]):
        print "removed"
    else:
        print "not removed"
    send_map_to_others(True, nmap)
    send_map_to_others(False, nmap)

def task_sn_init(task_d, nmap):


    print "timestore update"

    ts_inst = get_instance()

    ts_inst.ts = int(task_d["timestamp"])

    ts_inst.write()


switch = { 
"task_set_status": task_set_status,
"task_node_properties": task_node_properties,
 "task_health_check_fwd": task_health_check_fwd,
 "task_health_check_report": task_health_check_report,
"task_node_map_update": task_node_map_update,
"task_nm_repo_update": task_nm_repo_update,
"task_add_member": task_add_member,
"task_remove_member": task_remove_member,
"task_sn_init": task_sn_init 
}


def handle_tasks(nmap):

    task = get_next_task()

    while (len(task) > 0):
        
        try:
            task_d = json.loads(task)
        except:
            print "Error in json task"
            lg.error("BAD JSON - " + task)
            task = get_next_task()
            continue

        action = task_d["action"]

        fnstr = "task_" +action

        if not fnstr in switch:
            print "Not a valid task! -  ", task
            task = get_next_task()
            continue
 
        fn_ptr = switch[fnstr]


        retval = fn_ptr (task_d, nmap)
        
        task = get_next_task()
