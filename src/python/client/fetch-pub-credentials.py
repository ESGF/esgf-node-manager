import requests, json
from OpenSSL import crypto
from hashlib import md5
from time import time

from ConfigParser import RawConfigParser
from argparse import ArgumentParser

DEFAULT_CERT = '/etc/grid-security/hostcert.pem'
DEFAULT_KEY = '/etc/grid-security/hostkey.pem'

DIGEST = b'sha256'

from  base64 import b64encode

def main(insecure, server):

	key = crypto.load_privatekey(crypto.FILETYPE_PEM, open(DEFAULT_KEY).read())

	data = {}
	data['cert'] = open(DEFAULT_CERT).read()

	m = md5()
	m.update(str(time()))

	data['nonce'] = b64encode(m.digest())
	data['signature'] = b64encode(crypto.sign(key, data['nonce'], DIGEST))

	data['action'] = 'get_pub_config'

	url = 'https://' + server  + '/esgf-nm/api'

	verify = not insecure

	try:
		res = requests.post(url, data=data, verify=verify)
	except Exception as e:
		print e
		exit(-1)

	arr = json.loads(res.text)

	outstr = '\n'+'\n'.join(map(lambda x: ' | '.join(x), arr) )+'\n'

	pub_cfg_file = "/esg/config/esgcet/esg.ini.2"

	rcp = RawConfigParser()
	rcp.readfp(open(pub_cfg_file))
	
	rcp.add_section('config:cmip6')
	rcp.set('config:cmip6', 'pid_credentials', outstr)

	rcp.write(open(pub_cfg_file, 'w'))

if __name__ == '__main__':
    parser = ArgumentParser()
    # parser.add_option('-s', dest='data', help='Data to be signed')
    # parser.add_option('-c', dest='certificate', help='X.509 certificate path')
    # parser.add_option('-k', dest='key', help='RSA private key path')
    parser.add_argument('-i', '--insecure', action="store_true", help='Insecure operation (does not verify server)')
    parser.add_argument('server', type=str, help="Server FQDN")
    args = parser.parse_args()
    main(args.insecure, args.server)
