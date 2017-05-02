from simplequeue import RunningWrite


from django.http import HttpResponse
import json, os

from ConfigParser import ConfigParser 

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


def splitRecord(option, sep='|'):
    """Split a multi-line record in a .ini file.

    Returns a list of the form [[field, field, ...], [field, field, ...]]

    option
      String option in the .ini file.

    sep
      Separator character.

    For example, if the init file option is

    ::

        creator_options =
          Contact_1 | foo | http://bar
          Contact_2 | boz@bat.net | 

    then the code::

        lines = config.get(section, 'creator_options')
        result = splitRecord(lines)

    returns::

    [['Contact_1', 'foo', 'http://bar'], ['Contact_2', 'boz@bat.net', '']]

    """
    result = []
    for record in option.split('\n'):
        if record == '':
            continue
        fields = map(string.strip, record.split(sep))
        result.append(fields)

    return result

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

    elif action == "get_pub_config":


        f = open("/esg/config/esgcet/esg.ini")
        cp = ConfigParser()
        cp.readfp(f)

        if not "config:cmip6" in cp.sections():

            resp.code = "{ 'ERROR': 'Missing config section.  Contact server administrator.'}"
        if not 'pid_credentials':
            resp.code = "{ 'ERROR': 'Credentials not present.  Contact server administrator'}"


        resp_code = json.dumps(splitRecord(cp.get("config:cmip6", "pid_credentials"), indent=2))
        
    else:
        resp_code = "INVALID_REQ"

    if (len(task) > 0):
        rw = RunningWrite(task)
        rw.start()


    return HttpResponse(resp_code, content_type="text/plain")

