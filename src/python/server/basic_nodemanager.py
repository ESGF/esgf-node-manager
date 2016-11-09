import sys, os

import nodemgr.nodemgr.config_root as config_root

config_root.RUN_IN_DJ = False


from time import sleep, time

from supernode import member_node_check, supernode_check, links_check, supernode_init, my_turn, calc_time, check_properties, quick_check, send_map_to_others

from nodemgr.nodemgr.nodemap import get_instance as nm_get_instance
from taskhandler import handle_tasks

from time_store import get_instance as ts_get_instance

from nodemgr.nodemgr.site_profile import get_prop_st

from query import has_db, QueryRunner

import logging

from nodemgr.nodemgr.settings import MAP_FN, TIMESTAMP
from gen_nodemap import do_gen_nodemap


def usage():
    print "Usage:  python", sys.argv[0], "<node-map-file> [timestamp-file if SN]"
    exit(1)
    

#if (len(sys.argv) <2):
#    usage()



logger = logging.getLogger("esgf_nodemanager")

fh = logging.FileHandler("/esg/log/esgf_nm.log")
fh.setLevel(logging.ERROR)

logger.addHandler(fh)


if len(sys.argv) > 1:
    deploy_arg = sys.argv[1]

    if deploy_arg == "SPOT_DEPLOY":
                
        print "Fetching nodes list..."
        do_gen_nodemap(["INIT"])


nodemap_instance = nm_get_instance()


nodemap_instance.load_map(MAP_FN)

supernode = False


if (nodemap_instance.myid > 0) :
    timestore_instance = ts_get_instance()


    if len(TIMESTAMP) < 0:
        print "This node is running as a supernode; timestamp file parameter is missing"
        usage()
    timestore_instance.filename = TIMESTAMP
    if os.path.exists(TIMESTAMP):
        timestore_instance.restore()
    else:
        print "timestame file is missing!  Please fetch from distribution mirror"
        exit(1)

    supernode = True    


count = 0

QUANTA = 12

HEALTH_CHECK_TIME=2
LINK_CHECK_TIME=5

SLEEP_TIME= 5

MasterNode = (nodemap_instance.myid == "1")

supernode_count = len(nodemap_instance.nodemap["supernodes"])





#if MasterNode:
#
#    timestore_instance.ts = int(time())
#    supernode_init(nodemap_instance, timestore_instance.ts)

if (supernode):
    
    node_props = get_prop_st()
    nodemap_instance.set_prop(nodemap_instance.myname, node_props)

    if len(sys.argv) > 1:
        deploy_arg = sys.argv[1]

        if deploy_arg == "SPOT_DEPLOY":
            
            send_map_to_others(True, nodemap_instance)
            send_map_to_others(False, nodemap_instance)
            


from esg_config import EsgConfig

ec = EsgConfig()

from nodemgr.nodemgr.settings import metrics_fn as res_path

if has_db:

    qr = QueryRunner(res_path, ec)

    qr.start()


while (True):

    # this is the inital timestamp to be distributed to supernodes for determining when each performs its lead in the health check.  


        sleep(SLEEP_TIME)

    

#    if quick_check(): 

        handle_tasks(nodemap_instance)
    

        if (supernode):

            cur_ts = time()
            count = calc_time(cur_ts, timestore_instance.ts, QUANTA,  SLEEP_TIME) 
        
#        print "Count:" , count
    
            if count == HEALTH_CHECK_TIME:

        
                if timestore_instance.ts > 0 and my_turn(cur_ts - timestore_instance.ts, int(nodemap_instance.myid), supernode_count, QUANTA * SLEEP_TIME ):
                    print "SN check", count, cur_ts
                    supernode_check(nodemap_instance)
                
                if nodemap_instance.myid > -1:
                    member_node_check(nodemap_instance)
                
                

            if count == LINK_CHECK_TIME:

                if timestore_instance.ts > 0 and my_turn(cur_ts - timestore_instance.ts, int(nodemap_instance.myid), supernode_count, QUANTA * SLEEP_TIME ):
                    print "Status review", count, cur_ts
                    links_check(nodemap_instance)
                    print "start properties check"
                    check_properties(nodemap_instance)
                    print "end properties check"

        nodemap_instance.write_back()            
        supernode_count = len(nodemap_instance.nodemap["supernodes"])        

#    else:
#        print "failed check (apache issue)"

        sys.stdout.flush()
        sys.stderr.flush()


    
