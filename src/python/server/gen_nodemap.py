# Offline script to add supernode entries to node manager json configuration node map file

import json, sys, os

from httplib import HTTPConnection as Conn

from nodemgr.nodemgr.settings import MAP_FN

from time import time

from json import loads as load_json

PORT = 80


def write_json_file(in_fn, json_obj):

    outf = open(in_fn, "w")

    outs = ""
    
    if (isinstance(json_obj, str)):
        outs = json_obj
    else:
        json_obj["create_timestamp"] = time()
        outs = json.dumps(json_obj,  sort_keys=True, indent=4, separators=(',', ': '))


    outf.write(outs)
    outf.close()


def do_fetch_nodemap(fqdn):

    arr = []

    try:

        f = open ('/esg/config/esgf_supernodes_list.json')
        arr = load_json(f.read())
    except:
        pass
    
    data = None

    for host in arr:

        

        data_str = ""

        if host != fqdn:

            try:
                conn = Conn(host, PORT, timeout=10)
                conn.request("GET", "/esgf-nm/api?action=sync_node_map_file")
                resp = conn.getresponse()
                
                data_str = resp.read()
            
                data = json.loads(data_str)

                conn.close()
            

            

            except:
                pass
            
            if not data is None and len(data_str) > 10:
                write_json_file(MAP_FN, data_str) 
                return True
            
        
    print "Could not retrieve the node map from another site.  Will generate a fresh one.  Advised that you attempt to retrieve the map in order to have a complete view of member nodes in the federation." 
    return False




def do_gen_nodemap(args):

    print "running as", args    

    filename = MAP_FN

    if filename is None or len(filename) == 0:
        print "Error with mapfile setting"
        sys.exit(-1)

    got_map = False

    if (len(args) < 1):
        print "gen_roadmap requires argument. exiting"
        exit (-1)

    if args[0] != "INIT":
        got_map = do_fetch_nodemap(args[0])

    if got_map:
        print "Nodemap retrieval succeeded! exiting..."
        exit (0)

    sn_list = []

    old_members = []

    old_mcount = 0

    try:
        f = open(MAP_FN)
    
        existing_map  = json.loads(f.read())
        old_members = existing_map["membernodes"]
        old_mcount = existing_map["total_membernodes"]
    except:
        print "problem with existing map"
        pass

    if len(args) > 1 and args[0] != "INIT":  
    
        print "using command line arguments"
        sn_list = args
    else:
        try:
            f = open('/esg/config/esgf_supernodes_list.json')
    
            sn_list = json.loads(f.read())
        except:
            print "Error loading supernodes list file"
            sys.exit(-1)



    new_json = {}

    supernodes = []

    membernodes = []

    links = []

    i = 1


    for mm in sn_list:

        tmp_supernode = {}


        tmp_supernode["id"] = str(i)
        tmp_supernode["health"] = "new"
        tmp_supernode["hostname"] = mm

        supernodes.append(tmp_supernode)

        if len(old_members) >= i :
            membernodes.append(old_members[i-1])
        else :
            tmp_entry = {}
            tmp_entry["members"] = []
            tmp_entry["supernode"] = str(i)

            membernodes.append(tmp_entry)

        i = i + 1 
    


    new_json["membernodes"] = membernodes


    new_json["supernodes"] = supernodes

    sn_count = len(supernodes)

    new_json["total_supernodes"] = sn_count

    new_json["total_membernodes"] = old_mcount

    for fr in range(1, sn_count):

        for tt in range(fr + 1, sn_count+1):

            new_link = {}
        
            new_link["from"] = str(fr)
            new_link["to"] = str(tt)
            new_link["status"] = "unverified"
            new_link["speed"] = 0

            links.append(new_link)


    new_json["links"] = links

    write_json_file(filename, new_json)


if(__name__ == '__main__'):
    sys.exit(do_gen_nodemap(sys.argv[1:]))

    



