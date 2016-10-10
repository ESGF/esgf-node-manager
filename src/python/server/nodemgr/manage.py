#!/usr/bin/env python
import os
import sys


from time import time, sleep
from threading import Thread

#from nodemgr.healthcheck import get_node_list, RunningCheck


def do_work():

    hostname = os.uname()[1]

    for x in range(3):


        sleep(15)

        print ("check time on ", hostname)

        tarr = []

# refac0ir with urls.py block
        for n in get_node_list():


            if n != hostname:
                t = RunningCheck(n, True)
                t.start()
                tarr.append(t)

        for tt in tarr:

            tt.join()
            print tt.nodename, tt.eltime
    
    print "thread done"

if __name__ == "__main__":
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "nodemgr.settings")

    from django.core.management import execute_from_command_line

#    th = None
    
   #  if sys.argv[-1] == "master":
   #      print "master node setup"
   #      th = Thread(target = do_work)
   #      th.start()
        
    
   #      execute_from_command_line(sys.argv[0:-1])
   #  else:
   #      print "not master"
 
    execute_from_command_line(sys.argv)

   #  if (not th is None):

   #      th.join
   #      print "done"
