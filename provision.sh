#!/usr/bin/env bash
set -o nounset

function install {
  dpkg -l $1
  if [ $? -ne 0 ]; then
    echo "Installing $1 ..."
    sudo apt-get install -y $1
  fi
}

function install_mvn {
  ls -1 /opt/apache-maven-$1 &> /dev/null
  if [ $? -ne 0 ]; then
    echo "Installing Apache Maven $1"
    cd /tmp &&
    wget -q http://archive.apache.org/dist/maven/maven-3/$1/binaries/apache-maven-$1-bin.tar.gz &&
    cd /opt &&
    sudo tar -xzf /tmp/apache-maven-$1-bin.tar.gz

    echo "export PATH=/opt/apache-maven-$1/bin:$PATH" >> ~vagrant/.bashrc
  fi
}

apt-get update

install openjdk-17-jdk
install rpm
install wget
install_mvn 3.9.3

echo "Provisioning completed."
