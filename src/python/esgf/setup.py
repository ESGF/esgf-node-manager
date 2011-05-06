#!/usr/bin/env python
    
import sys, os, re

try:
    from setuptools import setup, find_packages
except ImportError:
    from ez_setup import use_setuptools
    use_setuptools()
    from setuptools import setup, find_packages

# Set the version string in esgf_node_manager.esgf.__init__.py
v = file(os.path.join(os.path.dirname(__file__), 'esgf_node_manager', '__init__.py'))
VERSION = re.compile(r".*__version__ = '(.*?)'", re.S).match(v.read()).group(1)
v.close()
print "esgf_node_manager version =", VERSION

setup(
    name = 'esgf_node_manager',
    version = VERSION,
    description = 'ESGF Node Manager Database Package',
    author = 'ESGF Software Team',
    author_email = 'esgf-mechanic@lists.llnl.gov',
    url = 'http://esgf.org',
    install_requires = ["esgcet>=2.8"],
    dependency_links = ["http://rainbow.llnl.gov/dist/externals"],
    packages = find_packages(exclude=['ez_setup']),
    include_package_data = True,
    package_data = {'esgf_node_manager.schema_migration': ['migrate.cfg'],
                    'esgf_node_manager.schema_migration.versions': ['*.sql'],
                    },
    scripts = ['scripts/esgf_node_manager_initialize',
               'scripts/esgf_accesslog',
               ],
    zip_safe = False,                   # Migration repository must be a directory
)
