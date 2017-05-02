import requests
from OpenSSL import crypto
from hashlib import md5
from time import time

from ConfigParser import ConfigParser
from argparse import OptionParser

DEFAULT_CERT = '/etc/grid-security/hostcert.pem'
DEFAULT_KEY = '/etc/grid-security/hostkey.pem'

DIGEST = b'sha256'

def main(insecure):

	key = crypto.load_privatekey(crypto.FILETYPE_PEM, open(DEFAULT_KEY).read())

	data = {}
	data['cert'] = open(DEFAULT_CERT).read()

	m = md5()
	m.update(str(time.time()))

	data['nonce'] = m.digest()
	data['signature'] = crypto.sign(key, data['nonce'], DIGEST)



	verify = not insecure

	res = requests.post(url, data=data, verify=verify)

	print res.text


if __name__ == '__main__':
    parser = ArgumentParser()
    # parser.add_option('-s', dest='data', help='Data to be signed')
    # parser.add_option('-c', dest='certificate', help='X.509 certificate path')
    # parser.add_option('-k', dest='key', help='RSA private key path')
    parser.add_argument('-i', '--insecure', action="store_true", help='Insecure operation (does not verify server)')

    parser.parse_args()
    main(args.i)