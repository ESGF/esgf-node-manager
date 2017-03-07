import os, stat
from threading import Thread

TASK_CODE = 0

MAX_TASK = 4096

MAX_COUNT = 256

#TASK_DIR = os.environ.get("ESGF_NM_TASKS")
from settings import TASK_DIR

if TASK_DIR is None or len(TASK_DIR) < 1:
    print "Need to set ESGF_NM_TASKS"
    exit(1)


TASK_PID = os.getpid()

# This module is not thread safe.  The intention is to use within single-threaded processes that have unique PID.  There should be a single consumer but multiple producers 

def inc_task_code():
    
    global TASK_CODE

    TASK_CODE = TASK_CODE + 1
    if TASK_CODE > MAX_TASK:
        TASK_CODE = 0


def get_task_fn():
    return TASK_DIR + "/task." + str(TASK_PID) + "." + str(TASK_CODE)
    


def write_task(data):
    

    inc_task_code()
    fn = get_task_fn()
    
    count = 0 
    # We want to ensure that the file we intend to write for the task does not already exists
    while ( os.path.exists(fn)):
        if count > MAX_COUNT :
            print "Error:  max task queue times exceeds expected.  Aborting"
            exit(1)
            
        count = count + 1
        
        inc_task_code()
        fn = get_task_fn()

    f = open(fn, "w" )
    f.write(data)

    f.close()
    os.chmod(fn, stat.S_IWGRP | stat.S_IRGRP | stat.S_IWUSR | stat.S_IRUSR  )
    
#    print os.stat(fn)


class RunningWrite(Thread):
    def __init__(self, data):
        super(RunningWrite, self).__init__()
        self.data = data
    def run(self):
        write_task(self.data)


def get_next_task():
    

    files = os.listdir(TASK_DIR)

    if (len(files) == 0):

        return ""

    times_fn = []

    for ff in files:
        fpath = TASK_DIR + "/" + ff
        mtime = os.path.getmtime(fpath)
        times_fn.append([mtime, fpath])
        
    new_times = sorted(times_fn, key=lambda x: x[0])

    next_task_fn = new_times[0][1]

    inf = open(next_task_fn)
    next_task_str = inf.read()
    inf.close()

    os.remove(next_task_fn)

    return next_task_str



    
    
