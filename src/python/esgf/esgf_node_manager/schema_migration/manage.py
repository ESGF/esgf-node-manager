#!/usr/bin/env python
from migrate.versioning.shell import main
main(url='postgresql://drach1:@localhost:5432/esgcet_new', debug='False', repository='/home/drach1/gitwork/esgf-node-manager/src/python/esgf/esgf_node_manager/schema_migration')
