 ------
 Maven RPM Plugin - Contributing
 ------
 Rickard von Essen
 ------
 25 December 2013
 -----

Contributing

  Create a patch against the subversion trunk and
  attach it to an issue in the bugtracker.

Using Vagrant

  Building and testing the plugin can easily be done with help of
  {{{http://www.vagrantup.com}Vagrant}} and
  {{{http://www.virtualbox.org}VirtualBox}}

  Install VirtualBox and Vagrant and run <<<vagrant up --provision>>>. This
  will start a VirtualBox machine running CentOS and install Maven, JDK, and
  rpm-build. It will share the <<<rpm-maven-plugin>>> directory available at
  <<</vagrant>>>.

  To log into the machine <<<vagrant ssh>>> or to do a mvn install do:
  <<<vagrant ssh -c "cd /vagrant && mvn clean install">>>.
