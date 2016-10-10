import os, json

from django.conf.urls import patterns, include, url
from django.contrib import admin

from django.http import HttpResponse #, QueryDict

from healthcheck import RunningCheck

from simplequeue import write_task
from nodemap import NodeMap, PROPS_FN
from api import nodemgrapi

from site_profile import get_prop_st, REG_FN, ts_func

global served 
served = False

from settings import metrics_fn, TS_THRESH

MET_FN = metrics_fn

url_prefix = ""

if not os.environ.get("NM_TEST_SRV") is None and os.environ.get("NM_TEST_SRV") == "true":
    url_prefix = "esgf-nm/"

# TODO - need to 
#hostname = os.uname()[1]

#checkarr = []
#MAPFILE = "/export/ames4/node_mgr_map.json"

#nodemap_instance = NodeMap(MAPFILE)


def make_json(ststr):

    return '{"action": "update_status", "status", "' + ststr+'"}"'





def write_resp(full):

    global served

    status = "LAPSED"

    if os.path.isfile(PROPS_FN):
        f = open(PROPS_FN)
        j = f.read()
        f.close()

        
        if len(j) > 2:
            dd = json.loads(j)
    
            ts_last = int(dd["ts_all"])
            ts_now = int(ts_func())

            ts_diff = ts_now - ts_last

            if ts_diff > TS_THRESH:
                status = "LAPSED"
            else:
                status = "GOOD"
        else:
            status = "LAPSED"

    met_resp = {}

    if os.path.isfile(MET_FN):
        f = open(MET_FN)
        met_in = f.read()
        try:
            met_resp = json.loads(met_in)
        except:
            print "JSON error with metrics file"
        f.close()


# TODO if need to not full handle that
#    if not served:  -- for now just serve all the data each tim
# TODO work on requests based on absense of data, with timestamp of last for comparison later

        


    if True:
        ret = get_prop_st()

#        print "first time, serving json"

        served = True

        met_resp["status"] = status
        ret.update(met_resp)

        string_resp = json.dumps(ret)

#        print string_resp
        return string_resp

    elif len(met_resp) > 0:
        met_resp["esgf.host"] = get_prop_st()["esgf.host"]
        met_resp["action"] = "node_properties"
        met_resp["status"] = status
        out_str = json.dumps(met_resp)
 #       print out_str
        return out_str
    else:
        return make_json(status)

def healthcheckreport(request):
    qd = request.GET

    outd = qd.copy()
    outd["action"] = "health_check_report"
    

    write_task(json.dumps(outd))

    return HttpResponse("")


def healthcheckack(request):
# TODO check timestamp and write resp when the prop file is more recent.

    qd = request.GET

    if "what" in qd and  qd["what"] == "timestamp":

        return HttpResponse('{"ts_all": '+str(ts_func())+'}',  content_type='text/json')
    

    if ("forward" in qd and qd["forward"] == "True"):

        outd = qd.copy()

        outd["action"] = "health_check_fwd"
        write_task(json.dumps(outd))
#        print "checking on others"        

    resp = write_resp(False)

    return HttpResponse(resp)
    

def get_json(request):
    
    resp = ""

    if os.path.isfile(PROPS_FN):
        f = open(PROPS_FN)
        resp = f.read()
        f.close()

    else:
#        print "no file"
        resp = "NO_FILE"

    return HttpResponse(resp, content_type='text/json')


def get_metrics(request):
    
    resp = ""

    if os.path.isfile(MET_FN):
        f = open(MET_FN)
        resp = f.read()
        f.close()

    else:
#        print "no file"
        resp = "NO_FILE"

    return HttpResponse(resp, content_type='text/json')

def get_reg_xml(request):
    f = open(REG_FN)
    
    resp = f.read()
    f.close()
    
    httpresp = HttpResponse(resp, content_type='text/xml')

    return httpresp

def hello_world(request):

    resp = "<h1>Hello World</h1>"

    return HttpResponse(resp)


urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'nodemgr.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    # url(r'^admin/', include(admin.site.urls)
                       url(r'^'+url_prefix+'health-check-api', healthcheckack),
                       url(r'^'+url_prefix+'health-check-rep', healthcheckreport),
                       url(r'^'+url_prefix+'api', nodemgrapi),
                       url(r'^'+url_prefix+'node-props.json', get_json),
                       url(r'^'+url_prefix+'metrics.json', get_metrics),
                       url(r'^'+url_prefix+'registration.xml', get_reg_xml),
                       url(r'^'+url_prefix, hello_world))

