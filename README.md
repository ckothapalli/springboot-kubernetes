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

### Test the application from outside of ECP
The application url is provided in the service description by the Annotations. We can access the application from the 
local machine.
```
(base) -bash-3.2$ curl http://m2-ess-vm196.mip.storage.hpecorp.net:10004/greeting
Hello! Current time is: Fri May 07 18:34:57 GMT 2021
```

## Network connectivity
There are many hops for the http request from the local machine to reach the application running in a container in the pod.
ECP provides a haproxy load balancer running in the Gateway node that routes the requests coming from outside. 
This load balancer forwards the requests to the Service object through the NodePort through the Service object 
to the Pod to the Container running in the Pod.

LoadBalancer -> NodePort -> Service -> Pod -> Container

### Accessing the application through the NodePort
In the first test, we accessed application through the LoadBalancer port.
Let's try accessing the application on the NodePort. The NodePort 30354 can be seen in the Service object and 
the host name is listed by the `get pods -o wide` command. In this network path, we are bypassing the load balancer.
```
(base) -bash-3.2$ curl http://m2-e910-201.mip.storage.hpecorp.net:30354/greeting
Hello! Current time is: Fri May 07 19:03:11 GMT 2021
```

### Can we access the application through the Service object?
I am not able to do this, but the Service object itself has the IP and port. 
The Cluster-IP (10.99.20.216) field when you list the services is the IP and port is 88 in our case. 
The IP can be seen in the service description as well.
The curl command fails on the Service IP:port, but telnet works. Probably the Service object does not 
understand http protocol
```
[root@m2-ess-vm198 ~]# curl http://10.99.20.216:88/greeting
^C
[root@m2-ess-vm198 ~]# telnet 10.99.20.216 88
Trying 10.99.20.216...
Connected to 10.99.20.216.
Escape character is '^]'.
```
### Port forwarding to Pod 
To test if the application is running in the Pod on port 8080, we can port forward from local Mac.
In the sample below, I am doing it from the K8s master node because I did not configure kubectl on Mac, but this can 
really be done from Mac as well.
```
[root@m2-ess-vm198 ~]# kubectl port-forward springboot-kubernetes-86c4f866fc-nlpc5 7773:8080
Forwarding from 127.0.0.1:7773 -> 8080
```

In another terminal, ssh to the master node and test the application:
```
[root@m2-ess-vm198 ~]# hostname
m2-ess-vm198.mip.storage.hpecorp.net
[root@m2-ess-vm198 ~]# curl http://localhost:7773/greeting
Hello! Current time is: Fri May 07 20:56:56 GMT 2021
```

### DNS
I need to understand this better, but there is a DNS server for the pods and services to talk to each other.
This can be seen if we login to any pod. Below, we first login to a pod and then issue nslookup to the Service 
object by its name. The resolve.conf file can also be seen. Also note the Service host full name. 
```
[root@m2-ess-vm198 ~]# kubectl exec -it springboot-kubernetes-86c4f866fc-nlpc5 -- /bin/sh
/ # nslookup springboot-kubernetes
Server:		10.96.0.10
Address:	10.96.0.10:53

Name:	springboot-kubernetes.default.svc.cluster.local
Address: 10.99.20.216

/ # cat /etc/resolv.conf
nameserver 10.96.0.10
search default.svc.cluster.local svc.cluster.local cluster.local mip.storage.hpecorp.net
options ndots:5
/ #
```

### To check the logs of the application running in the container
Truncated the log. As there is only one container in the pod, we don't need to specify the conatiner name.
```
[root@m2-ess-vm198 ~]# kubectl logs --since=24h springboot-kubernetes-86c4f866fc-nlpc5

2021-05-07 04:01:39.777  INFO 1 --- [           main] c.e.k.s.SpringbootKubernetesApplication  : Starting SpringbootKubernetesApplication v0.0.1-SNAPSHOT on springboot-kubernetes-86c4f866fc-nlpc5 with PID 1 (/app.jar started by root in /)
2021-05-07 04:01:41.213  INFO 1 --- [           main] c.e.k.s.SpringbootKubernetesApplication  : Started SpringbootKubernetesApplication in 1.757 seconds (JVM running for 2.146)
In WelcomeController.greeting
```
If we want, we can find the container name using `kubectl describe pod <pod name>` and specify that in the command.
```
kubectl logs springboot-kubernetes-86c4f866fc-nlpc5 springboot-kubernetes
```

### To login to the pod
```
kubectl exec -it springboot-kubernetes-86c4f866fc-nlpc5 -- /bin/sh

/ # ps -ef
PID   USER     TIME  COMMAND
    1 root      1:05 java -jar app.jar
```
We can also login to the docker container directly. Login to the worker node 201 and run the below.
```
[root@m2-e910-201 ~]# docker ps
CONTAINER ID   IMAGE                                              COMMAND                  CREATED        STATUS        PORTS     NAMES
3306e8f8e3f9   10.163.168.91:443/choudary/springboot-kubernetes   "java -jar app.jar"      17 hours ago   Up 17 hours             k8s_springboot-kubernetes_springboot-kubernetes-86c4f866fc-nlpc5_default_54635e7c-f3fe-4d86-afaf-1527ca3ca8cf_1

[root@m2-e910-201 ~]# docker exec -it 3306e8f8e3f9 /bin/bash
bash-5.0# ls /
app.jar  bin  dev  etc	home  lib  lib64  media  mnt  opt  proc  root  run  sbin  srv  sys  tmp  usr  var
bash-5.0# ps ef
PID   USER     TIME  COMMAND
    1 root      1:06 java -jar app.jar
```