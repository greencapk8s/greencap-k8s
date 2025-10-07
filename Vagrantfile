Vagrant.configure("2") do |config|
  config.vm.box = "bento/ubuntu-22.04"
  config.vm.network "public_network", use_dhcp_assigned_default_route: true, bridge: "wlp3s0"
  # config.vm.network "forwarded_port", guest: 30001, host: 8080, id: "k8s-http"
  # config.vm.network "forwarded_port", guest: 30002, host: 8443, id: "k8s-https"

  # PS.: In script `create-enviroment-gui.sh` we set the memory and cpus to 4096 and 4.
  # VBoxManage modifyvm $VM_NAME --memory 4096 --cpus 4
  # The vagrantfile is not able to set the memory and cpus via the provider.
  config.vm.provider "VirtualBox" do |vb|
    vb.name = "greencap-k8s"
    vb.memory = ENV["MEM_SIZE"] || 2048
    vb.cpus = ENV["CPUS"] || 2
  end

  # Declare environment variables.
  with_gui = ENV["WITH_GUI"] == "1"
  install_browser = ENV["INSTALL_BROWSER"] == "1"
  setup_kind_k8s = ENV["SETUP_KIND_K8S"] == "1"

  # Install docker and running simple hello-world.
  config.vm.provision "docker" do |d|
    d.run "hello-world"
  end

  config.vm.provision "shell", inline: <<-SHELL
    sudo apt update
    sudo DEBIAN_FRONTEND=noninteractive apt install -y python3-pip python3-venv
    sudo apt install -y apt-transport-https
  SHELL

  if with_gui
    config.vm.provision "shell", inline: <<-SHELL
      echo "[GUI] Installing Xubuntu Core + LightDM..."
      echo "lightdm shared/default-x-display-manager select lightdm" | sudo debconf-set-selections
      sudo DEBIAN_FRONTEND=noninteractive apt install -y xubuntu-core lightdm
      sudo systemctl enable lightdm
      sudo systemctl set-default graphical.target
    SHELL
  end

  if install_browser
    config.vm.provision "shell", inline: <<-SHELL
      echo "[Browser] Installing firefox..."
      sudo apt install -y firefox
    SHELL
  end

  if setup_kind_k8s
    config.vm.provision "shell", inline: <<-SHELL
      mkdir -p $HOME/greencap
    SHELL

    # Copy files.
    config.vm.provision "file", source: "./installers", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./apache-hello", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./ingress-nginx", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./kubernetes-dashboard", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./projects", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./harbor", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./monitoring", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./helm-values", destination: "$HOME/greencap/"
    config.vm.provision "file", source: "./pgadmin", destination: "$HOME/greencap/"

    # Run install tools.
    config.vm.provision "shell", inline: <<-SHELL
      echo "🚗 Running installer scripts..."
      cd /home/vagrant/greencap
      USER_NAME_INSTALL="vagrant" ./installers/run-installers.sh
    SHELL
  end
end
