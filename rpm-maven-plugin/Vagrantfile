# -*- mode: ruby -*-
# vi: set ft=ruby :
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # Default provider VirtualBox
  config.vm.box = "CentOS-6.5-x86_64"
  config.vm.box_url = "https://github.com/2creatives/vagrant-centos/releases/download/v6.5.1/centos65-x86_64-20131205.box"

  config.vm.provider "virtualbox" do |vb|
      vb.memory = 2048
      vb.customize ["modifyvm", :id, "--ioapic", "on", "--cpus", 2]
  end
  config.vm.provider "parallels" do |prl|
      prl.customize ["set", :id, "--memsize", 1024]
      prl.customize ["set", :id, "--cpus", 2]
  end

  config.vm.provision "shell", path: "provision.sh"
end
