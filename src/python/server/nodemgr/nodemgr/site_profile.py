from time import time

PROPERTIES = "/esg/config/esgf.properties"
TYPE_FN = "/esg/config/config_type"

REG_FN = "/esg/config/registration.xml"


properties_struct = None



from types import DictType

def get_prop_st():
    return properties_struct

def ts_func():

    return str(int(time()*1000))

def parse_properties():

    pdict = {}

    f = open(PROPERTIES)

    pdict["timestamp"] = ts_func()
    for line in f:
        ll = line.strip()
        
        if len(ll) > 0 and ll[0] != '#':
            parts = line.split('=')
            pdict[ parts[0].strip() ] = parts[1].strip()

    f.close()

    f = open(TYPE_FN)
    val=f.read()
    pdict["node.type"] = val.strip()
    pdict["action"] = "node_properties"
    f.close()


    return pdict


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
        outarr.append(x["esg.root.id"])
        outarr.append('" namespace="') 
        outarr.append(x["node.namespace"])
        outarr.append('" nodePeerGroup="')
        outarr.append(x["node.peer.group"]) 
        outarr.append('" supportEmail="')
        outarr.append(x["mail.admin.address"]) 
        outarr.append('" hostname="')
        outarr.append(x["esgf.host"]) 
        outarr.append('" ip="')
        outarr.append(x["esgf.host.ip"]) 
        outarr.append('" shortName="')
        outarr.append(x["node.short.name"]) 
        outarr.append('" longName="')
        outarr.append(x["node.long.name"]) 
        outarr.append('" timeStamp="')
# generated on properties file read
        outarr.append(x["timestamp"]) 
        outarr.append('" version="')
        outarr.append(x["version"]) 
        outarr.append('" release="')
        outarr.append(x["release"]) 
        outarr.append('" nodeType="')
        outarr.append(x["node.type"]) 
        outarr.append('" adminPeer="')
#  This should correspond to super-node for member-nodes
#  adjacent super-node for super-nodes
#        outarr.append(x["admin.peer"]) 
        outarr.append(x["esgf.default.peer"]) 
        
        outarr.append('" defaultPeer="')
        outarr.append(x["esgf.default.peer"]) 

#        outarr.append('" ="')
#        outarr.append(x[""]) 
        outarr.append('">\n')

# TODO: populate CA hash and correct endpoint
# for now uses the hostname
        outarr.append('        <CA hash="dunno" endpoint="')
        outarr.append(x["esgf.host"])
        outarr.append('" dn="dunno"/>\n')

        if "node.geolocation.lat" in x:
            outarr.append('       <GeoLocation lat="')
            outarr.append(x["node.geolocation.lat"])
            outarr.append('" lon="')
            outarr.append(x["node.geolocation.lon"])
            if "node.geolocation.city" in x:
                outarr.append('" city="')
                outarr.append(x["node.geolocation.city"])
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


#        <FrontEnd endpoint="http://pcmdi9.llnl.gov/esgf-web-fe/"/>
        if "index.service.endpoint" in x:
            outarr.append('        <IndexService port="80" endpoint="')
            outarr.append(x["index.service.endpoint"]) 
            outarr.append('"/>\n')


#        <IndexService port="8983" endpoint="http://pcmdi9.llnl.gov/esg-search/search"/>

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
            outarr.append(str(x["download.bytes"]))
            outarr.append('" users="')
            outarr.append(str(x["download.users"]))
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

#TODO get public cert
        outarr.append("   <PEMCert>\n         <Cert>NOT_AVAILABLE</Cert>\n   </PEMCert>\n")



        outarr.append("    </Node>\n")
    outarr.append("</Registration>\n")
    return ''.join(outarr)    

properties_struct = parse_properties()
