from threading import Thread
from time import time, sleep

from nodemap import get_instance

from httplib import HTTPConnection as Conn
from httplib import HTTPException

from simplequeue import write_task

import os, logging

logger = logging.getLogger("esgf_nodemanager")
fh = logging.FileHandler("/esg/log/esgf_nm_dj.log")
fh.setLevel(logging.ERROR)
logger.addHandler(fh)


import json

# def init_node_list():

#     org_list = [ "aims1.llnl.gov" ]

#     for n in range(1,4):

#         org_list.append("greyworm"+ str(n) + ".llnl.gov")

#     return org_list

# node_list = init_node_list()

# def get_node_list():
#     return node_list


localhostname = os.uname()[1]

# MAPFILE = "/export/ames4/node_mgr_map.json"


from settings import PORT


class RunningCheck(Thread):

    def __init__(self, nodename, fwdcheck, first=False, checkarr=None, fromnode=""):
        super(RunningCheck, self).__init__()
        self.nodename = nodename
        self.fwdcheck = fwdcheck
        self.eltime = -1
        self.first = first
        self.checkarr = checkarr
        self.fromnode = fromnode
        self.logger = logging.getLogger("esgf_nodemanager")

    def handle_resp(self, resp):
        buf = resp.read()


        if len(buf) > 10:

#            print "longer response"

            foo = None
            try: 
                foo = json.loads(buf)
#                print foo["esgf.host"], foo["status"]

            except:
                print "Error loading json resopnse"
                print buf
                print vars(self)
                print

            if not foo is None:
                write_task(buf)

#        else:
#            pass
#            print "short response" + buf


    def run(self):

        ts = time()
#        print "Health check on", self.nodename

        conn = Conn(self.nodename, PORT, timeout=30)


        eltime = -1
        error = ""
        try:
            conn.request("GET", "/esgf-nm/health-check-api?from=" + localhostname + "&forward=" + str(self.fwdcheck))
        
            resp = conn.getresponse()

            if resp.status == 500:
                print "500 error" 
                self.logger.error(resp.read())
            
            eltime = time() - ts
            
            self.handle_resp(resp)



        except Exception as e:
            error = "connectivity problem"
            print e

        self.eltime = eltime

        nodemap_instance = get_instance()
        node_list = nodemap_instance.get_supernode_list()

        if not self.fwdcheck:
            
            if not self.checkarr is None:
                self.checkarr.append(self.nodename + "=" + str(eltime))

            if (self.first):
                if len(node_list) > len(self.checkarr) + 2:
                    sleep(.01)
                conn = Conn(self.fromnode, PORT, timeout=30)
                url = "/esgf-nm/health-check-rep?from=" + localhostname

                for n in self.checkarr:
                    url = url + "&" + n
                    
                error = ""
                resp = ""
                try:
                    conn.request("GET", url)
                    resp = conn.getresponse()
                    self.handle_resp(resp)

                except Exception as e:
                    error = "connectivity problem"
                    print e
                

# def do_checks(fwdcheck):

#     tarr = []

#     for n in nodemap_instance.get_supernode_list():


#         if n != hostname:
#                 t = RunningCheck(n, True)
#                 t.start()
#                 tarr.append(t)



#     if (fwdcheck):
    
#         for tt in tarr:

#             tt.join()
#             tt.hostname, tt.eltime
