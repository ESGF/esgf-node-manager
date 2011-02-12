#!/usr/bin/env python
    
try:
    from setuptools import setup, find_packages
except ImportError:
    from ez_setup import use_setuptools
    use_setuptools()
    from setuptools import setup, find_packages

VERSION = '0.1.3'

setup(
    name = 'esgf',
    version = VERSION,
    description = 'ESGF publication package',
    author = 'PCMDI Software Team',
    author_email = 'webmaster@pcmdi.llnl.gov',
    url = 'http://esg-pcmdi.llnl.gov',
    install_requires = ["psycopg2>=2.0", "SQLAlchemy>=0.5.3", "sqlalchemy_migrate>=0.6"],
    setup_requires = ["psycopg2>=2.0", "SQLAlchemy>=0.5.3", "sqlalchemy_migrate>=0.6"],
    dependency_links = ["http://www-pcmdi.llnl.gov/dist/externals"],
    packages = find_packages(exclude=['ez_setup']),
    include_package_data = True,
    package_data = {'esgf.schema_migration': ['migrate.cfg'],
                    'esgf.schema_migration.versions': ['*.sql'],
                    },
    scripts = ['scripts/esgf_initialize',
               ],
    zip_safe = False,                   # Migration repository must be a directory
)
