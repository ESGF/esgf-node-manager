import requests

REFRESH_TIME = 60


def member_node_fetch_xml(count, nodemap_instance):
"""
	Used to pull down the latest registration.xml instance from the supernode

	count
	  The running loop count used to determine if time to refresh
	nodemap_instance
	  a NodeMap object 
"""
	if count == REFRESH_TIME:
		count = 0
	elif ((count % REFRESH_TIME) == (REFRESH_TIME / 2)):
		sn_id="0"

		if nodemap_instance.myid == -1:
			nodemap_instance.set_member_id()
			if nodemap_instance.myid == -1: 
				raise Exception("Error in member node id.  Please report this to support.")
		for ll in nodemap_instance.nodemap["membernodes"]:


		    for mm in ll['members']:

				if mm['id'] == nodemap_instance.myid:
					sn_id = ll['supernode']
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
			print "ERROR retrieving registration xml from primary supernode.  consider re-peering", sn_hostname, e
			return count

		if resp is None:
			print "ERROR retrieving registration xml from primary supernode.  consider re-peering", sn_hostname
			return count

		if resp.status_code != 200:
			print "ERROR retrieving registration xml from primary supernode.  consider re-peering  Status:", resp.status_code, sn_hostname
			return count
		try:
			with open('/esg/config/registration.xml', 'w') as f:
				f.write(resp.text)	
		except Exception as e:
			print "Error writing registration xml", e



	return count
