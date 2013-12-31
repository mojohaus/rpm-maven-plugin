#!/usr/bin/env bash
set -o nounset

function install {
  rpm -qa | grep -q $1
  if [ $? -ne 0 ]; then
    echo "Installing $1 ..."
    sudo yum install -y $1
  fi
}

function install_mvn {
  ls -1 /opt/apache-maven-$1 &> /dev/null
  if [ $? -ne 0 ]; then
    echo "Installing Apache Maven $1"
    cd /tmp &&
    wget -q http://apache.mirrors.spacedump.net/maven/maven-2/$1/binaries/apache-maven-$1-bin.tar.gz &&
    cd /opt &&
    tar -xzf /tmp/apache-maven-$1-bin.tar.gz

    echo "export PATH=/opt/apache-maven-$1/bin:$PATH" >> ~vagrant/.bashrc
  fi
}

install java-1.6.0-openjdk
install rpm-build
install_mvn 2.2.1

echo "Provisioning completed."
