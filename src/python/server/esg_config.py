# File to manage reading the esgf type configuration
CONFIG_FN = '/esg/config/config_type'

DATA_BIT = 4
INDEX_BIT = 8
IDP_BIT = 16
COMPUTE_BIT = 32



class EsgConfig():
    """
   Encapsulates the types of node deployments.  Supports Data and IdP for the expected presence of particular database schemata
    """
    def __init__(self):

        val = 0

        try:
            f = open(CONFIG_FN)
            val = int(f.read().strip())
        except:
            print "Error opening config type file!"
            exit(-1)
        self.config_type = val

    def is_idp(self):
        """
    Returns true if the node is set as IdP
        """
        return IDP_BIT & self.config_type


    def is_data(self):
        """
    Returns true if the node is set as Data node
        """

        return DATA_BIT & self.config_type

