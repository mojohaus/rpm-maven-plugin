# -*- mode: ruby -*-
# vi: set ft=ruby :
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # Default provider VirtualBox
  config.vm.box = "ubuntu/jammy64"

  config.vm.provider "virtualbox" do |vb|
      vb.memory = 2048
      vb.customize ["modifyvm", :id, "--ioapic", "on", "--cpus", 2]
  end
  config.vm.provider "parallels" do |prl|
      prl.customize ["set", :id, "--memsize", 1024]
      prl.customize ["set", :id, "--cpus", 2]
  end
  config.vm.synced_folder ".", "/vagrant"

  config.vm.provision "shell", path: "provision.sh"
end
