from threading import Thread
from time import time, sleep

from nodemap import get_instance

import requests

from simplequeue import write_task

import os, logging

from config_root import RUN_IN_DJ



logger = logging.getLogger("esgf_nodemanager")

fh = None

if RUN_IN_DJ:
    fh = logging.FileHandler("/esg/log/esgf_nm_dj.log")
else:
    fh = logging.FileHandler("/esg/log/esgf_nm.log")


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


from settings import PROTO

class BasicSender(Thread):

    def __init__(self):
        super(BasicSender, self).__init__()
        self.target = ""

    def mkurl(self, stn):

        if self.target == "":

            raise Exception("No target server configured")

        return PROTO + "://" + self.target + stn 


class RunningCheck(BasicSender):

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
        buf = resp.text


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

        eltime = -1
        error = ""
        try:
            self.target = self.nodename
            resp = requests.get(self.mkurl("/esgf-nm/health-check-api?from=" + localhostname + "&forward=" + str(self.fwdcheck)), verify='/etc/grid-security/certificates/')
        
            if resp.status_code == 500:
                print "500 error" 
                self.logger.error(resp.text)
            elif resp.status_code != 200:
                pass

            eltime = time() - ts
            
            self.handle_resp(resp)



        except Exception as e:
            error = "connectivity problem"
            print "Connectvity Problem:", e, self.target 

        self.eltime = eltime

        nodemap_instance = get_instance()
        node_list = nodemap_instance.get_supernode_list()

        if not self.fwdcheck:
            
            if not self.checkarr is None:
                self.checkarr.append(self.nodename + "=" + str(eltime))

            if (self.first):
                if len(node_list) > len(self.checkarr) + 2:
                    sleep(.01)
                self.target = self.fromnode
                url = self.mkurl("/esgf-nm/health-check-rep?from=" + localhostname)

                for n in self.checkarr:
                    url = url + "&" + n
                    
                error = ""
                resp = ""
                try:
                    resp = requests.get(url, verify='/etc/grid-security/certificates/')



                    self.handle_resp(resp)

                except Exception as e:
                    error = "connectivity problem"
                    print "Connectvity Problem:", e, self.target
                

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
