#!/bin/sh

#the argument being passed in indicates how many versions will exist
#during an upgrade, this value will be 1, in which case we do not want to stop
#the service since the new version will be running once this script is called
#during an uninstall, the value will be 0, in which case we do want to stop 
#the service and remove the /etc/init.d script.
if [ "$1" = "0" ]
then
    if [ -s "/etc/init.d/myapp" ]
    then
        /etc/init.d/myapp stop
        rm /etc/init.d/myapp
    fi
fi;