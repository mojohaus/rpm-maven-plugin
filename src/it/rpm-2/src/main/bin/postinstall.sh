#!/bin/sh

#create soft link script to services directory
ln -s /usr/myusr/app/bin/start.sh /etc/init.d/myapp

chmod 555 /etc/init.d/myapp