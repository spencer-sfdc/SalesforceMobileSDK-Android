# workaround for docker limitation with Ubuntu packaging system
sudo dpkg-divert --local --rename --add /sbin/initctl
sudo ln -s /bin/true /sbin/initctl

sudo apt-get install python-software-properties
sudo add-apt-repository ppa:saltstack/salt
sudo apt-get update
sudo apt-get --assume-yes install salt-master
sudo apt-get --assume-yes install salt-minion
sudo apt-get --assume-yes install salt-syndic

