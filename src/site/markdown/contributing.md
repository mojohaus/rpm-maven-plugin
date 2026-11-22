# Contributing

Fork the [GitHub repository](https://github.com/mojohaus/rpm-maven-plugin) and open a pull request, or
report a problem in the [issue tracker](https://github.com/mojohaus/rpm-maven-plugin/issues).

# Using Vagrant

Building and testing the plugin can easily be done with help of
[Vagrant](https://www.vagrantup.com) and
[VirtualBox](https://www.virtualbox.org)

Install VirtualBox and Vagrant and run `vagrant up --provision`. This
will start a VirtualBox machine running CentOS and install Maven, JDK, and
rpm-build. It will share the `rpm-maven-plugin` directory available at
`/vagrant`.

To log into the machine `vagrant ssh` or to do a mvn install do:
`vagrant ssh -c "cd /vagrant && mvn clean install"`.
