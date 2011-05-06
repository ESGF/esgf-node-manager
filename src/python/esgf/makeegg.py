#!/usr/bin/env python

import sys
import os
import getopt
import shutil

usage = """Usage:

    makeegg.py [Options] version

    Create a new esgf_node_manager package as an egg file.

Options:

    -h: Help

    -v: Verbose

"""

HOME = os.environ['HOME']
GITWORK = os.path.join(HOME, "gitwork")
GITBRANCH = "devel-drach"
ESGCET = os.path.join(GITWORK, "esgf-node-manager/src/python/esgf")
VERSION_BASE = "__init__.py"
VERSION_DIR = os.path.join(ESGCET, "esgf_node_manager")
VERSION_FILE = os.path.join(VERSION_DIR, VERSION_BASE)
DIST = os.path.join(ESGCET, "dist")
EXTERNALS = os.path.join(GITWORK, "esg-dist")
VERSION_INFO = """"ESGF node manager modules"
__version__ = '%s'
"""

def main(argv):

    try:
        args, lastargs = getopt.getopt(argv, "hv")
    except getopt.error:
        print sys.exc_value
        print usage
        sys.exit(0)

    if len(lastargs)!=1:
        print usage
        sys.exit(0)
    version = lastargs[0]

    verbose = False
    bdistout = "/dev/null"
    for flag, arg in args:
        if flag=='-h':
            print usage
            sys.exit(0)
        elif flag=='-v':
            verbose = True
            bdistout = "/dev/stdout"
        
    os.chdir(ESGCET)

    # Update version in esgcet/__init__.py
    print "Updating", VERSION_FILE
    f = open(VERSION_FILE, 'w+')
    f.write(VERSION_INFO%version)
    f.close()

    # Commit version to git repo
    gitadd = "git add %s"%VERSION_FILE
    gitcommit = "git commit"
    gitpush = "git push origin %s"%GITBRANCH
    print gitadd
    os.system(gitadd)
    print gitcommit
    os.system(gitcommit)
    print gitpush
    os.system(gitpush)

    # Create the egg with setup.py bdist_egg
    os.chmod("setup.py", 0755)
    os.system("setup.py --verbose bdist_egg > %s"%bdistout)

    # Copy the egg to the git distribution directory
    maj,min, dum, dum, dum = sys.version_info
    egg_base = "esgf_node_manager-%s-py%s.%s.egg"%(version, maj, min)
    egg_file = os.path.join(DIST, egg_base)
    egg_externals = os.path.join(EXTERNALS, egg_base)

    print "Copying %s to %s"%(egg_file, EXTERNALS)
    shutil.copy(egg_file, EXTERNALS)

    # Post the egg
    os.chdir(EXTERNALS)
    postcommand = "postfiles %s"%egg_base
    print postcommand
    os.system(postcommand)

if __name__=='__main__':
    main(sys.argv[1:])
