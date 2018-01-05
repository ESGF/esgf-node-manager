# TODO refactor into modular script
import sys, os, requests
from time import time
from json import loads as load_json


def main(argv):
"""
    main func 

    argv - expects sys.argv or alternate representation of arguments

"""
    cmd = argv[1]
    myname = os.uname()[1]

    from time import time

    # doFetch = False

    # nodemap_obj = None

    # try:

    #     f = open('/esg/config/esgf_nodemgr_map.json')
    #     nodemap_obj = load_json(f.read())
    #     f.close()

    # except:
    #     print "Missing a Nodemap file.  Will attempt to fetch from closest supernode."
    #     doFetch = True
    
    if cmd == "add":

        f = open ('/esg/config/esgf_supernodes_list.json')
        arr = load_json(f.read())

        if myname in arr:

            print "This server is a Super-node.  Exiting..."
            exit(0)

        times_arr = []

        for host in arr:

            resp =None
            st = time()
            dt = 999

            url = "https://" + host + "/esgf-nm"

            try: 

                resp = requests.get(url, verify=False)
                et = time()
                if not resp is None and resp.status_code == 200:
                    dt = et - st

            except:
                print host, "unresponsive, skipping"
            
            times_arr.append([dt, host])

        sorted_arr = sorted(times_arr)

        if len(sorted_arr)  == 0:
            print
            return

        target = sorted_arr[0][1]
        proj = argv[2]
        stdby = argv[3]

        resp = None
        url = "https://" + target + "/esgf-nm/api?action=add_member&from=" + myname + "&project=" + proj + "&standby=" + stdby
         
        try:

            resp = requests.get(url, verify=False)
            if resp.status_code == 200:   
                print "Contacted ", target, " wait for ping-back"
            else:
                print "WARNING:  " + target + " returned status ", resp.status_code, " - please contact the ESGF site administrator"

        except Exception as e:
            print "ERROR in connect to", target, str(e)

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
    

if __name__ == "__main__":

    if len(sys.argv) < 4:

        print "Usage: ", sys.argv[0], " <add|delete> <project(s)> <standby?>"
        exit (0)

    main(sys.argv)        
    
