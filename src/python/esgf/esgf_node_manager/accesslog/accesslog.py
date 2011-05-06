from esgcet.publish import Hessian, RemoteCallException

def getAccessLogService(serviceURL, port, certfile, keyfile):
    service = Hessian(serviceURL, port, key_file=keyfile, cert_file=certfile)
    return service

def getAccessLog(starttime, endtime, serviceURL, certfile, keyfile, port=443):
    service = getAccessLogService(serviceURL, port, certfile, keyfile)
    try:
        metadata = service.fetchAccessLogData(starttime, endtime)
    except:
        print 'SSL error: is the certificate file valid?'
        raise
    return metadata

def pingAccessLogService(serviceUrl, certfile, keyfile, port=443):
    pass
