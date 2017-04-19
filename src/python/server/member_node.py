import requests

REFRESH_TIME = 60

def member_node_fetch_xml(count, nodemap_instance):

	if count == REFRESH_TIME:
		count = 0
	elif ((count % REFRESH_TIME) == (REFRESH_TIME / 2)):
		sn_id="0"
		for ll in nodemap_instance.nodemap["membernodes"]:

		    sn_id = ll['supernode']
		    for mm in ll['members']:

		    	mm['id'] == nodemap_instance.myid
		    	break

		if sn_id == "0":
		    raise Exception('Error:  No supernode has been assigned.  Please run "membernode_cmd add"')


		sn_hostname = ""

		for ll in nodemap_instance.nodemap["supernodes"]:
			if ll["id"] == sn_id:
				sn_hostname = ll["hostname"]

		resp = None

		try:
			resp = requests.get("https://" + sn_hostname + "/esgf-nm/registration.xml", verify='/etc/grid-security/certificates')
		except Exception as e:
			print "ERROR retrieving registration xml from primary supernode.  consider re-peering", e
			return count

		if resp is None:
			print "ERROR retrieving registration xml from primary supernode.  consider re-peering"
			return count

		if resp.status_code != 200:
			print "ERROR retrieving registration xml from primary supernode.  consider re-peering  Status:", resp.status_code
			return count
		try:
			with open('/esg/config/registration.xml', 'w') as f:
				f.write(resp.text)	
		except Exception as e:
			print "Error writing registration xml", e



	return count
