#!/bin/sh

if [ -s "/etc/init.d/myapp" ]
then
    /etc/init.d/myapp stop
    rm /etc/init.d/myapp
fi
