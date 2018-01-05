from time import time

OUT_PROP_LIST = "/esg/config/esgf_reg_props.json"
PROPERTIES = "/esg/config/esgf.properties"
TYPE_FN = "/esg/config/config_type"

REG_FN = "/esg/config/registration.xml"


CERT_FN = "/etc/grid-security/hostcert.pem"

properties_struct = None

from types import DictType
def get_prop_st():

    return properties_struct.pdict

def ts_func():

    return str(int(time()*1000))


def get_cert():

    try:
        f  = open(CERT_FN)
        return f.read()
    except:
        print "ERROR: default Globus/Gridftp certificate file not found or not readable."      


    return "NOT_AVAILABLE"

class EsgfProperties:


    def __init__(self):

        pdict = {}
        
        try:
            f = open(PROPERTIES)

            pdict["timestamp"] = ts_func()
            for line in f:
                ll = line.strip()
        
                if len(ll) > 0 and ll[0] != '#':
                    parts = line.split('=')
                    key = parts[0].strip()

                    val = parts[1].strip()
                        # for security/privacy we don't want abs. paths on the host filesystem

                    if len(val) > 2 and val[0] != '/':
                        pdict[ key ] = val

            f.close()

            f = open(TYPE_FN)
            val=f.read()
            pdict["node.type"] = val.strip()
            pdict["action"] = "node_properties"
            f.close()
        except Exception as e:
            print "Error opening properties", e

        self.pdict = pdict


     
    def __dict__(self, x):
        return pdict.get(x)

    def get(self,x):
        return pdict.get(x)

#def add_sn(pdict,sn):
    




def gen_reg_xml(arr_in):

#    print str(arr_in)

    outarr =  ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n']
    
#    outarr = []

    ts = ts_func()
    outarr.append('<Registration timeStamp="')
    outarr.append(ts)
    outarr.append('">\n')

    for key in arr_in:
        

        x = arr_in[key]

        if key == "ts_all":
            continue

        if not type(x) is DictType:
            print "Type issue"
            print str(x)
            continue

        outarr.append("    <Node ")

        outarr.append('organization="' )
        outarr.append(x.get("esg.root.id","esg.root.id"))
        outarr.append('" namespace="')
        outarr.append(x.get("node.namespace","node.namespace"))
        outarr.append('" nodePeerGroup="')
        outarr.append(x.get("node.peer.group","node.peer.group"))
        outarr.append('" supportEmail="')
        outarr.append(x.get("mail.admin.address","mail.admin.address"))
        outarr.append('" hostname="')
        outarr.append(x.get("esgf.host","esgf.host"))
        outarr.append('" ip="')
        outarr.append(x.get("esgf.host.ip","esgf.host.ip"))
        outarr.append('" shortName="')
        outarr.append(x.get("node.short.name","node.short.name"))
        outarr.append('" longName="')
        outarr.append(x.get("node.long.name","node.long.name"))
        outarr.append('" timeStamp="')
# generated on properties file read
        outarr.append(x.get("timestamp","timestamp"))
        outarr.append('" version="')
        outarr.append(x.get("version","version"))
        outarr.append('" release="')
        outarr.append(x.get("release","release"))
        outarr.append('" nodeType="')
        outarr.append(x.get("node.type","node.type"))
        outarr.append('" adminPeer="')
#  This should correspond to super-node for member-nodes
#  adjacent super-node for super-nodes
#        outarr.append(x.get("admin.peer","admin.peer"))
        outarr.append(x.get("esgf.default.peer","esgf.default.peer"))

        outarr.append('" defaultPeer="')
        outarr.append(x.get("esgf.default.peer","esgf.default.peer"))

#        outarr.append('" ="')
#        outarr.append(x[""])
        outarr.append('">\n')

        # make x[] more resilient  - SKA: is resillient when using "blah" in x
    
# TODO: populate CA hash and correct endpoint
# for now uses the hostname
        outarr.append('        <CA hash="dunno" endpoint="')
        outarr.append(x.get("esgf.host","esgf.host") )
        outarr.append('" dn="dunno"/>\n')

        if "node.geolocation.lat" in x:
            outarr.append('       <GeoLocation lat="')
            outarr.append(x.get("node.geolocation.lat", "NA"))
            outarr.append('" lon="')
            outarr.append(x.get("node.geolocation.lon", "NA"))
            if "node.geolocation.city" in x:
                outarr.append('" city="')
                outarr.append(x.get("node.geolocation.city", "NA" ))
            outarr.append('"/>\n')

        if "node.manager.service.endpoint" in x:
            outarr.append('        <NodeManager endpoint="')
            outarr.append(x["node.manager.service.endpoint"]) 
            outarr.append('"/>\n')

        if "orp.security.authorization.service.endpoint" in x:
            outarr.append('        <AuthorizationService endpoint="')
            outarr.append(x["orp.security.authorization.service.endpoint"]) 
            outarr.append('"/>\n')

        if "idp.service.endpoint" in x:
             outarr.append('       <OpenIDProvider endpoint="')
             outarr.append(x["idp.service.endpoint"]) 
             outarr.append('"/>\n')

#        <OpenIDProvider endpoint="https://pcmdi9.llnl.gov/esgf-idp/idp/openidServer.htm"/>

#  TODO - should add a CoG service endpoint ? 
#        <FrontEnd endpoint="http://pcmdi9.llnl.gov/esgf-web-fe/"/>
        if "index.service.endpoint" in x:
            outarr.append('        <IndexService port="80" endpoint="')
            outarr.append(x["index.service.endpoint"]) 
            outarr.append('"/>\n')


# TODO get groups for attribute service
#        <AttributeService endpoint="https://pcmdi9.llnl.gov/esgf-idp/saml/soap/secure/attributeService.htm">

#        outarr.append('" ="')
#        outarr.append(x[""]) 
#        outarr.append('">\n')


        if "thredds.service.endpoint" in x:
            outarr.append('       <ThreddsService enpoint="')
            outarr.append(x["thredds.service.endpoint"]) 
            outarr.append('"/>\n')

        # <ThreddsService endpoint="http://dapp2p.cccma.ec.gc.ca/thredds"/>


        # <GridFTPService endpoint="gsiftp://dapp2p.cccma.ec.gc.ca">
        #     <Configuration serviceType="Replication" port="2812"/>
        #     <Configuration serviceType="Download" port="2811"/>
        # </GridFTPService>

        if "gridftp.service.endpoint" in x:
            outarr.append('<GridFTPService endpoint="')
            outarr.append(x["gridftp.service.endpoint"])

            outarr.append('">\n')
            outarr.append('<Configuration serviceType="Replication" port="2812"/>')
            outarr.append('<Configuration serviceType="Download" port="2811"/>')
            outarr.append('         </GridFTPService>\n')

# TODO metricz - download count size users , registered users



                

        outarr.append('     <Metrics>\n')
        
        if "download.count" in x:
            outarr.append('       <DownloadedData count="')
            outarr.append(str(x["download.count"]))
            outarr.append('" size="')
            outarr.append(str(x.get("download.bytes", "NA")))
            outarr.append('" users="')
            outarr.append(str(x.get("download.users","NA")))
            outarr.append('"/> \n')
        if "users_count" in x:
            outarr.append('       <RegisteredUsers count="')
            outarr.append(str(x["users_count"]))
            outarr.append('"/>\n')
        else:
            outarr.append('       <RegisteredUsers count="0"/>\n')
        outarr.append('     </Metrics>\n')

        if "orp.service.endpoint" in x:
            outarr.append('        <RelyingPartyService endpoint="')
            outarr.append(x["orp.service.endpoint"]) 
            outarr.append('"/>\n')            

        #cert_str = get_cert()  # this can be updated an any time so best to reload

        outarr.append("   <PEMCert>\n         <Cert>NA</Cert>\n   </PEMCert>\n")
        
        outarr.append("    </Node>\n")
    outarr.append("</Registration>\n")
    return ''.join(outarr)    

properties_struct = EsgfProperties()
