import sys, os

from httplib import HTTPConnection as Conn


from time import time
from json import loads as load_json


if len(sys.argv) < 4:

    print "Usage: ", sys.argv[0], " <add|delete> <project(s)> <standby?>"
    exit (0)

cmd = sys.argv[1]

myname = os.uname()[1]

from nodemgr.nodemgr.settings import PORT

print "Port", PORT

from time import time

if cmd == "add":


#    f = open("/esg/config/esgf_nodemgr_map.json")

#    nodemap = json.loads(f.read())

#    for entry in nodemap["supernodes"]:

    #    target = sys.argv[2]

    f = open ('/esg/config/esgf_supernodes_list.json')
    arr = load_json(f.read())


    times_arr = []

    for host in arr:

        resp =None
        st = time()
        dt = 999

        try: 
            conn = Conn(host, PORT, timeout=30)
            conn.request("GET", "/esgf-nm")
            resp = conn.getresponse()
            conn.close()

        except:
            pass

        et = time()

        if not resp is None and resp.status == 200:

            dt = et - st
        
        times_arr.append([dt, host])

    sorted_arr = sorted(times_arr)

    target = sorted_arr[0][1]
    proj = sys.argv[2]
    stdby = sys.argv[3]

    resp = None
    conn = Conn(target, PORT, timeout=30)
    
    try:
        conn.request("GET", "/esgf-nm/api?action=add_member&from=" + myname + "&project=" + proj + "&standby=" + stdby)
        resp = conn.getresponse()
        conn.close()
        print "Contacted ", target, " wait for ping-back"
    except:
        print "Error in connect to found node"

elif cmd ==  "delete":
    conf = "/esg/config/esgf_nodemgr_map.json"

    f = open(conf)

    target = ""

    targetnum = 0
    
    nodemap = load_json(f.read())

    for entry in nodemap["membernodes"]:

        if "members" in entry:

            for mn in entry["members"]:
                if mn["hostname"] == myname:
                    targetnum = int(entry["supernode"])
                    break
        if targetnum > 0:
            for sn in nodemap["supernodes"]:
                if int(sn["id"]) == targetnum:
                    target = sn["hostname"]
                    break
            
            break
        

    if target == "":
        print "An error has occurred.  The supernode managing this node not found in the node map."
        exit

        
    print "Contacting", target, "to delete self from federation"
    conn = Conn(target, PORT, timeout=30)

    conn.request("GET", "/esgf-nm/api?action=remove_member&from=" + myname )
    resp = conn.getresponse()
    conn.close()
    


    
    
