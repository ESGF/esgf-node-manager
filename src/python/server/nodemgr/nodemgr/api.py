from simplequeue import RunningWrite


from django.http import HttpResponse
import json, os

#import pdb

from nodemap import get_instance

nodemap_instance = get_instance()
from settings import MAP_FN
#MAP_FN = os.environ.get("ESGF_NODEMGR_MAP")

if MAP_FN is None or len(MAP_FN) < 2:
    print "Need to set ESGF_NODEMGR_MAP"
    exit

nodemap_instance.load_map(MAP_FN)
nodemap_instance.set_ro()

def nodemgrapi(request):
    
#    print "API request"

#    pdb.set_trace()

    resp_code="OK"

    qd = {}


    if    request.META.get('REQUEST_METHOD') == "GET":

        qd = request.GET
    elif   request.META.get('REQUEST_METHOD') == "POST":
        qd = request.POST
    else:
        return HttpResponse("METHOD_NOT_SUPPORTED")  

    try:
        action = qd["action"]
    except:
        return HttpResponse("INVALID_URL")
        
    task = ""

    if action == "get_nodes_status":

        nodemap_instance.reload()
        resp_code = nodemap_instance.get_indv_node_status_json()

    elif action == "sync_node_map_file":
        
        nodemap_instance.reload()

        resp_code = json.dumps(nodemap_instance.nodemap, sort_keys=True, indent=4, separators=(',', ': '))
        
    elif action in ["add_member", "remove_member", "sn_init", "set_status"]:

        task = json.dumps(qd)

    

    elif action in ["node_map_update", "nm_repo_update"]:

        print "update map!"

        data = request.body
        outd = qd.copy()
        outd["update"] = data

        task = json.dumps(outd)

        
    else:
        resp_code = "INVALID_REQ"

    if (len(task) > 0):
        rw = RunningWrite(task)
        rw.start()


    return HttpResponse(resp_code, content_type="text/plain")

