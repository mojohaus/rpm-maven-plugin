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
    wget -q http://archive.apache.org/dist/maven/maven-3/$1/binaries/apache-maven-$1-bin.tar.gz &&
    cd /opt &&
    sudo tar -xzf /tmp/apache-maven-$1-bin.tar.gz

    echo "export PATH=/opt/apache-maven-$1/bin:$PATH" >> ~vagrant/.bashrc
  fi
}

install java-1.8.0-openjdk-devel
install rpm-build
install wget
install_mvn 3.2.5

echo "Provisioning completed."
