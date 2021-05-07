# springboot-kubernetes
### To export and import the docker images from local Mac to the K8s master node
docker build -t springboot-kubernetes:2.0 .
docker save -o /tmp/springboot-k8sV2.tar springboot-kubernetes:2.0

scp /tmp/springboot-k8sV2.tar root@10.163.168.253:/tmp

On the K8s master node 253:
docker load -i /tmp/springboot-k8sV2.tar
[root@m2-ess-vm198 ~]# docker images
REPOSITORY                             TAG         IMAGE ID       CREATED          SIZE
springboot-kubernetes                  2.0         e85d672d59a2   17 minutes ago   171MB

### Tag it to the private docker repo (on K8s master node)
docker tag springboot-kubernetes:2.0 10.163.168.91:443/choudary/springboot-kubernetes:2.0

### Add the private registry IP to NO_PROXY list on master node and restart docker
[root@m2-ess-vm198 ~]# cat /etc/systemd/system/docker.service.d/http-proxy.conf
[Service]
Environment="HTTP_PROXY=http://web-proxy.corp.hpecorp.net:8080"
Environment="HTTPS_PROXY=http://web-proxy.corp.hpecorp.net:8080"
#Environment="NO_PROXY=$no_proxy"
#Environment="NO_PROXY=localhost,127.0.0.1,10.163.168.248,10.163.168.249,10.163.168.250,10.163.168.25[1-2],10.163.168.25[3-5],10.163.169.20[1-3],.mip.storage.hpecorp.net"
Environment="NO_PROXY=localhost,127.0.0.1,10.163.168.248,10.163.168.249,10.163.168.250,10.163.168.251,10.163.168.252,10.163.168.253,10.163.168.254,10.163.168.255,10.163.169.201,10.163.169.202,10.163.169.203,10.163.169.204,10.163.169.205,10.163.169.206,10.163.169.207,10.163.169.208,10.163.169.209,10.163.169.210,10.163.169.211,10.163.169.212,.mip.storage.hpecorp.net,10.163.168.91"

systemctl daemon-reload
systemctl restart docker

On the master node, copy the ca.crt file from the registry machine to the location below. In the below example, it is copied from another worker node to the master node
scp root@10.163.169.201:/etc/docker/certs.d/10.163.168.91:443/ca.crt /etc/docker/certs.d/10.163.168.91\:443/
[root@m2-ess-vm198 ~]# ls /etc/docker/certs.d/10.163.168.91:443
ca.crt

### Push to the private docker registry
Now we will be able to push the docker registry
docker push 10.163.168.91:443/choudary/springboot-kubernetes:2.0

[root@m2-ess-vm198 ~]# docker images
REPOSITORY                                         TAG         IMAGE ID       CREATED          SIZE
10.163.168.91:443/choudary/springboot-kubernetes   2.0         e85d672d59a2   30 minutes ago   171MB
springboot-kubernetes                              2.0         e85d672d59a2   30 minutes ago   171MB

