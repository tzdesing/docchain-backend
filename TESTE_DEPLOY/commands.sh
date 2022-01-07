#!/bin/bash 

sudo apt-get -qq update -y 
sudo apt-get -qq -y install openjdk-8-jre
sudo apt install unzip -y 
sudo apt-get remove docker docker-engine docker.io -y 
sudo apt install docker.io -y 
sudo groupadd docker 
sudo usermod -aG docker $USER
newgrp docker
sudo chown $(whoami) -R ./*
sudo apt install docker-compose -y 