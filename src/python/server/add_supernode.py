# Offline script to add supernode entries to node manager json configuration node map file

import json, sys, os

if len(sys.argv) < 2:  
    print "need <hostname> argument"
    exit(-1)


from nodemgr.nodemgr.settings import MAP_FN

if MAP_FN is None or len(MAP_FN) < 1:
    print "Error in setting"
    exit(-1)

f = open(MAP_FN)
obj = json.loads(f.read())
f.close()

ll = obj["links"]

total = obj["total_membernodes"] + obj["total_supernodes"]

new_num = total + 1

tmp_supernode = {}

tmp_supernode["id"] = str(new_num)
tmp_supernode["health"] = "new"
tmp_supernode["hostname"] = sys.argv[1]

sn_ids = []

for n in obj["supernodes"]:
    sn_ids.append(n["id"])


obj["supernodes"].append(tmp_supernode)

obj["total_supernodes"] = len(obj["supernodes"])

tmp_entry = {}
tmp_entry["members"] = []
tmp_entry["supernode"] = str(new_num)

obj["membernodes"].append(tmp_entry)

for n in sn_ids:
    new_link = {}
    
    new_link["from"] = n
    new_link["to"] = str(new_num)
    new_link["status"] = "unverified"
    new_link["speed"] = 0

    obj["links"].append(new_link)


outf = open(MAP_FN, "w")
outs = json.dumps(obj,  sort_keys=True, indent=4, separators=(',', ': '))
outf.write(outs)
outf.close()


    



