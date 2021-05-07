# springboot-kubernetes

### The hosts in K8s cluster:

![](./screenshots/K8s_hosts.png)

### To export and import the docker images from local Mac to the K8s master node
```
docker build -t springboot-kubernetes:2.0 .
docker save -o /tmp/springboot-k8sV2.tar springboot-kubernetes:2.0

scp /tmp/springboot-k8sV2.tar root@10.163.168.253:/tmp
```
On the K8s master node 253:
```
docker load -i /tmp/springboot-k8sV2.tar
[root@m2-ess-vm198 ~]# docker images
REPOSITORY                             TAG         IMAGE ID       CREATED          SIZE
springboot-kubernetes                  2.0         e85d672d59a2   17 minutes ago   171MB
```
### Tag it to the private docker repo (on K8s master node)
```
docker tag springboot-kubernetes:2.0 10.163.168.91:443/choudary/springboot-kubernetes:2.0
```

### Add the private registry IP to NO_PROXY list on master node and restart docker
```
[root@m2-ess-vm198 ~]# cat /etc/systemd/system/docker.service.d/http-proxy.conf
[Service]
Environment="HTTP_PROXY=http://web-proxy.corp.hpecorp.net:8080"
Environment="HTTPS_PROXY=http://web-proxy.corp.hpecorp.net:8080"
#Environment="NO_PROXY=$no_proxy"
#Environment="NO_PROXY=localhost,127.0.0.1,10.163.168.248,10.163.168.249,10.163.168.250,10.163.168.25[1-2],10.163.168.25[3-5],10.163.169.20[1-3],.mip.storage.hpecorp.net"
Environment="NO_PROXY=localhost,127.0.0.1,10.163.168.248,10.163.168.249,10.163.168.250,10.163.168.251,10.163.168.252,10.163.168.253,10.163.168.254,10.163.168.255,10.163.169.201,10.163.169.202,10.163.169.203,10.163.169.204,10.163.169.205,10.163.169.206,10.163.169.207,10.163.169.208,10.163.169.209,10.163.169.210,10.163.169.211,10.163.169.212,.mip.storage.hpecorp.net,10.163.168.91"

systemctl daemon-reload
systemctl restart docker
```

On the master node, copy the ca.crt file from the registry machine to the location below. In the below example, it is copied from another worker node to the master node
```
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
```

### Deploy the 'deployment' and 'service' on the master node
Check the yaml files in the resources folder. 
```
kubectl apply -f deployment.yml
kubectl apply -f service.yml

[root@m2-ess-vm198 ~]# kubectl get po -o wide
NAME                                     READY   STATUS    RESTARTS   AGE   IP            NODE                                  NOMINATED NODE   READINESS GATES
springboot-kubernetes-86c4f866fc-nlpc5   1/1     Running   1          15h   10.192.3.30   m2-e910-201.mip.storage.hpecorp.net   <none>           <none>
  
[root@m2-ess-vm198 ~]# kubectl get services
NAME                    TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
springboot-kubernetes   NodePort    10.99.20.216   <none>        88:30354/TCP   15h
  
[root@m2-ess-vm198 ~]# kubectl describe service springboot-kubernetes
Name:                     springboot-kubernetes
Namespace:                default
Labels:                   hpecp.hpe.com/hpecp-internal-gateway=true
Annotations:              hpecp-internal-gateway/88: m2-ess-vm196.mip.storage.hpecorp.net:10004
Selector:                 app=springboot-kubernetes
Type:                     NodePort
IP Families:              <none>
IP:                       10.99.20.216
IPs:                      10.99.20.216
Port:                     springboot-kubernetes-greeting  88/TCP
TargetPort:               8080/TCP
NodePort:                 springboot-kubernetes-greeting  30354/TCP
Endpoints:                10.192.3.30:8080
Session Affinity:         None
External Traffic Policy:  Cluster
Events:                   <none>
```


