from simplequeue import RunningWrite


from django.http import HttpResponse

import json, os, string

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

from OpenSSL import crypto

DIGEST = b'sha256'
cacert_path = '/etc/grid-security/certificates/'

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
    resp_status = 200

    qd = {}

    method = request.META.get('REQUEST_METHOD')

    if   method  == "GET":

        qd = request.GET
    elif  method == "POST":
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

        data = request.body
        outd = qd.copy()
        outd["update"] = data

        task = json.dumps(outd)

    elif action == "get_pub_config":

        signature = qd['signature'].decode('base64')
        data = qd['nonce']
        certificate = crypto.load_certificate(crypto.FILETYPE_PEM, str(qd['cert']))
        
        try:
            crypto.verify(certificate, signature, data, DIGEST)
        except Exception as e:

            return HttpResponse("{ 'ERROR': 'signature verification' , 'Message': " + str(e) + "}", status=401, content_type="text/plain")

        try:
        # Create a truststore

            store = crypto.X509Store()

# TODO - get hash list from a deployed file (refresh from distribution mirror)

            for cacert_filename in ['99eb76fc.0', '0597e90c.0', 'cd6ccc41.0', 'c6645765.0']:

                cacert_file = cacert_path + cacert_filename
                cert = crypto.load_certificate(crypto.FILETYPE_PEM, open(cacert_file).read())
    #            print cert.get_subject()
                store.add_cert(cert)

            # Verify a certificate
            store_ctx = crypto.X509StoreContext(store, certificate)
            store_ctx.verify_certificate()

        except Exception as e:
            return HttpResponse("{ 'ERROR': 'certificate verification' , 'Message': " + str(e) + "}", status=401, content_type="text/plain")

        f = open("/esg/config/esgcet/esg.ini")
        cp = ConfigParser()
        cp.readfp(f)

        if not "config:cmip6" in cp.sections():

            resp_code = "{ 'ERROR': 'Missing config section.  Contact server administrator.'}"
            resp_status = 500
        if not 'pid_credentials':
            resp_code = "{ 'ERROR': 'Credentials not present.  Contact server administrator'}"
            resp_status = 500

        resp_code = json.dumps(splitRecord(cp.get("config:cmip6", "pid_credentials")), indent=2)
        
    else:
        resp_code = "INVALID_REQ"

    if (len(task) > 0):
        rw = RunningWrite(task)
        rw.start()


    return HttpResponse(resp_code, content_type="text/plain", status=resp_status)

