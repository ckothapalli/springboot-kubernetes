# springboot-kubernetes

### The hosts in K8s cluster:

![](./screenshots/K8s_hosts.png)

### To export and import the docker images from local Mac to the K8s master node
```
docker build -t springboot-kubernetes:volume_mount .
docker save -o /tmp/springboot-k8s-volume_mount.tar springboot-kubernetes:volume_mount

scp /tmp/springboot-k8s-volume_mount.tar root@10.163.168.253:/tmp
```
On the K8s master node 253:
```
docker load -i /tmp/springboot-k8s-volume_mount.tar
[root@m2-ess-vm198 ~]# docker images
REPOSITORY                                         TAG            IMAGE ID       CREATED          SIZE
springboot-kubernetes                              volume_mount   271c17895546   17 minutes ago   171MB
```
### Tag it to the private docker repo (on K8s master node)
```
docker tag springboot-kubernetes:volume_mount 10.163.168.91:443/choudary/springboot-kubernetes:volume_mount
```

### Push to the private docker registry
Now we will be able to push the docker registry
```
docker push 10.163.168.91:443/choudary/springboot-kubernetes:volume_mount
```

### Deploy the 'deployment' and 'service' on the master node
Check the yaml files in the resources folder. 
Create a new namespace volume-mount first and create the deployment and service in that namespace.
```
kubectl create namespace volume-mount
kubectl apply -f deployment.yml --namespace=volume-mount
kubectl apply -f service.yml --namespace=volume-mount
```
#### Deployment description

```
[root@m2-ess-vm198 volume_mount]# kubectl get deployment -n volume-mount
NAME                    READY   UP-TO-DATE   AVAILABLE   AGE
springboot-kubernetes   2/2     2            2           <invalid>

[root@m2-ess-vm198 volume_mount]# kubectl describe deployment springboot-kubernetes -n volume-mount
Name:                   springboot-kubernetes
Namespace:              volume-mount
CreationTimestamp:      Sun, 09 May 2021 13:45:11 -0700
Labels:                 app=springboot-kubernetes
Annotations:            deployment.kubernetes.io/revision: 1
Selector:               app=springboot-kubernetes
Replicas:               2 desired | 2 updated | 2 total | 2 available | 0 unavailable
StrategyType:           RollingUpdate
MinReadySeconds:        0
RollingUpdateStrategy:  25% max unavailable, 25% max surge
Pod Template:
  Labels:  app=springboot-kubernetes
  Containers:
   springboot-kubernetes:
    Image:        10.163.168.91:443/choudary/springboot-kubernetes:volume_mount
    Port:         8080/TCP
    Host Port:    0/TCP
    Environment:  <none>
    Mounts:
      /tmp/host from host-tmp (rw)
      /tmp/pod_temp from content (rw)
  Volumes:
   content:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
   host-tmp:
    Type:          HostPath (bare host directory volume)
    Path:          /tmp
    HostPathType:
Conditions:
  Type           Status  Reason
  ----           ------  ------
  Available      True    MinimumReplicasAvailable
  Progressing    True    NewReplicaSetAvailable
OldReplicaSets:  springboot-kubernetes-578f5d7df (2/2 replicas created)
NewReplicaSet:   <none>
Events:
  Type    Reason             Age    From                   Message
  ----    ------             ----   ----                   -------
  Normal  ScalingReplicaSet  5m15s  deployment-controller  Scaled up replica set springboot-kubernetes-578f5d7df to 2
```

#### Pod description  
```
[root@m2-ess-vm198 volume_mount]# kubectl get po -n volume-mount
NAME                                    READY   STATUS    RESTARTS   AGE
springboot-kubernetes-578f5d7df-7668b   1/1     Running   0          <invalid>
springboot-kubernetes-578f5d7df-7p6xt   1/1     Running   0          <invalid>

[root@m2-ess-vm198 volume_mount]# kubectl describe po springboot-kubernetes-578f5d7df-7668b -n volume-mount
Name:         springboot-kubernetes-578f5d7df-7668b
Namespace:    volume-mount
Priority:     0
Node:         m2-e910-201.mip.storage.hpecorp.net/10.163.169.201
Start Time:   Sun, 09 May 2021 13:45:11 -0700
Labels:       app=springboot-kubernetes
              pod-template-hash=578f5d7df
Annotations:  cni.projectcalico.org/podIP: 10.192.3.34/32
              cni.projectcalico.org/podIPs: 10.192.3.34/32
              kubernetes.io/psp: hcp-psp-privileged
Status:       Running
IP:           10.192.3.34
IPs:
  IP:           10.192.3.34
Controlled By:  ReplicaSet/springboot-kubernetes-578f5d7df
Containers:
  springboot-kubernetes:
    Container ID:   docker://cb6ab00d6781b4d63c20280e6363cbaebf0a5bdbebc8288504ac1eba198b048a
    Image:          10.163.168.91:443/choudary/springboot-kubernetes:volume_mount
    Image ID:       docker-pullable://10.163.168.91:443/choudary/springboot-kubernetes@sha256:01f1fcd5ec145932c3a25b3c73639d01276fe778fb90a97c031d96c374809b8b
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Sun, 09 May 2021 13:45:12 -0700
    Ready:          True
    Restart Count:  0
    Environment:    <none>
    Mounts:
      /tmp/host from host-tmp (rw)
      /tmp/pod_temp from content (rw)
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-96p8n (ro)
Conditions:
  Type              Status
  Initialized       True
  Ready             True
  ContainersReady   True
  PodScheduled      True
Volumes:
  content:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
  host-tmp:
    Type:          HostPath (bare host directory volume)
    Path:          /tmp
    HostPathType:
  default-token-96p8n:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  default-token-96p8n
    Optional:    false
QoS Class:       BestEffort
Node-Selectors:  <none>
Tolerations:     node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                 node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type    Reason     Age   From               Message
  ----    ------     ----  ----               -------
  Normal  Scheduled  45s   default-scheduler  Successfully assigned volume-mount/springboot-kubernetes-578f5d7df-7668b to m2-e910-201.mip.storage.hpecorp.net
  Normal  Pulling    45s   kubelet            Pulling image "10.163.168.91:443/choudary/springboot-kubernetes:volume_mount"
  Normal  Pulled     45s   kubelet            Successfully pulled image "10.163.168.91:443/choudary/springboot-kubernetes:volume_mount" in 557.892414ms
  Normal  Created    45s   kubelet            Created container springboot-kubernetes
  Normal  Started    45s   kubelet            Started container springboot-kubernetes
```

#### Service description
Note that there is a warning message in the Events section and there is also no Annotation
showing the url for the service.
```
[root@m2-ess-vm198 volume_mount]# kubectl get service -n volume-mount
NAME                    TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
springboot-kubernetes   NodePort   10.107.206.14   <none>        80:30909/TCP   9m
[root@m2-ess-vm198 volume_mount]# kubectl describe service springboot-kubernetes  -n volume-mount
Name:                     springboot-kubernetes
Namespace:                volume-mount
Labels:                   hpecp.hpe.com/hpecp-internal-gateway=true
Annotations:              <none>
Selector:                 app=springboot-kubernetes
Type:                     NodePort
IP Families:              <none>
IP:                       10.107.206.14
IPs:                      10.107.206.14
Port:                     springboot-kubernetes-greeting  80/TCP
TargetPort:               8080/TCP
NodePort:                 springboot-kubernetes-greeting  30909/TCP
Endpoints:                10.192.3.33:8080,10.192.3.34:8080
Session Affinity:         None
External Traffic Policy:  Cluster
Events:
  Type     Reason  Age                   From         Message
  ----     ------  ----                  ----         -------
  Warning  HpeCp   4m52s (x17 over 10m)  hpecp-agent  Failed to query HPECP K8S services
```
The Controller service was down for some reason and that might have caused the above issue. 
Once the ECP Controller is restarted, the service worked just fine.
```
[root@m2-ess-vm198 ~]# kubectl describe service springboot-kubernetes -n volume-mount
Name:                     springboot-kubernetes
Namespace:                volume-mount
Labels:                   hpecp.hpe.com/hpecp-internal-gateway=true
Annotations:              hpecp-internal-gateway/80: m2-ess-vm196.mip.storage.hpecorp.net:10005
Selector:                 app=springboot-kubernetes
Type:                     NodePort
IP Families:              <none>
IP:                       10.103.23.122
IPs:                      10.103.23.122
Port:                     springboot-kubernetes-greeting  80/TCP
TargetPort:               8080/TCP
NodePort:                 springboot-kubernetes-greeting  31991/TCP
Endpoints:                10.192.3.33:8080,10.192.3.34:8080
Session Affinity:         None
External Traffic Policy:  Cluster
Events:                   <none>
```
### Test the application from outside of ECP
The application url is provided in the service description by the Annotations. We can access the application from the 
local machine.
The first request is to get the greeting from the stateless app. This would help to validate 
that the application is working fine.<br>
The second request is to POST a greeting to the application and store it to the `emptyDir` 
volume.<br> 
The third request reads the greetings from the file saved in the pod and displays 
all greetings.
```
(base) -bash-3.2$ curl http://m2-ess-vm196.mip.storage.hpecorp.net:10005/emptydir/greeting
Hello! Current time is: Mon May 10 14:59:56 GMT 2021

(base) -bash-3.2$ curl -X POST -d 'Welcome'  http://m2-ess-vm196.mip.storage.hpecorp.net:10005/emptydir/post_greetings
Welcome= saved successfully

(base) -bash-3.2$ curl http://m2-ess-vm196.mip.storage.hpecorp.net:10005/emptydir/get_all_greetings
["Hello stored in host:springboot-kubernetes-578f5d7df-7p6xt on Mon May 10 15:01:00 GMT 2021 in file /tmp/pod_temp/greetingsFile.txt",
"Welcome= stored in host:springboot-kubernetes-578f5d7df-7p6xt on Mon May 10 15:04:48 GMT 2021 in file /tmp/pod_temp/greetingsFile.txt"]
```

### emptyDir volume type
We can see the file stored in the pod at the location of the mount path `mountPath: /tmp/pod_temp`
specified in the deployment.yml for the `emptyDir` volume. 

```
      volumes:
        - name: content
          emptyDir: {}
      containers:
        - name: springboot-kubernetes
          image: 10.163.168.91:443/choudary/springboot-kubernetes:volume_mount
          ports:
            - containerPort: 8080
          volumeMounts:
            - name: content
              mountPath: /tmp/pod_temp  #A /pod_temp folder created inside a pod that persists across container restarts. Application should write to this folder


[root@m2-ess-vm198 ~]# kubectl get po -n volume-mount -o wide
NAME                                    READY   STATUS    RESTARTS   AGE   IP            NODE                                  NOMINATED NODE   READINESS GATES
springboot-kubernetes-578f5d7df-7668b   1/1     Running   0          18h   10.192.3.34   m2-e910-201.mip.storage.hpecorp.net   <none>           <none>
springboot-kubernetes-578f5d7df-7p6xt   1/1     Running   0          18h   10.192.3.33   m2-e910-201.mip.storage.hpecorp.net   <none>           <none>

[root@m2-ess-vm198 ~]# kubectl exec -it springboot-kubernetes-578f5d7df-7p6xt -n volume-mount -- /bin/sh

/ # cat /tmp/pod_temp/greetingsFile.txt
Hello stored in host:springboot-kubernetes-578f5d7df-7p6xt on Mon May 10 15:01:00 GMT 2021 in file /tmp/pod_temp/greetingsFile.txt
Welcome= stored in host:springboot-kubernetes-578f5d7df-7p6xt on Mon May 10 15:04:48 GMT 2021 in file /tmp/pod_temp/greetingsFile.txt
```
Note that another greeting posted to the same URL is saevd to a different pod because this
deployment has 2 pod replicas. The volume is local to the pod and the `get_all_greetings` 
request returns the greetings from the pod where the request is directed to by the Service.
```
(base) -bash-3.2$ curl -X POST -d 'How are you?'  http://m2-ess-vm196.mip.storage.hpecorp.net:10005/emptydir/post_greetings
How+are+you%3F= saved successfully

[root@m2-ess-vm198 ~]# kubectl exec -it springboot-kubernetes-578f5d7df-7668b -n volume-mount -- /bin/sh
/ # cat /tmp/pod_temp/greetingsFile.txt
How+are+you%3F= stored in host:springboot-kubernetes-578f5d7df-7668b on Mon May 10 15:04:12 GMT 2021 in file /tmp/pod_temp/greetingsFile.txt
```

### hostPath volume type
Instead of the storing the application data in `emptyDir` type of volume that goes away when
the pod dies, we can store the data in the worker node. This volume type is called `hostPath`. 
This volume type survives pod's death though
it works only if the new instance of the same pod is assigned to the same worker node. This
problem can go away if the mount path is present on all workers and points to an external 
storage device like NAS. <br>

```
    spec:
      volumes:
        - name: host-tmp
          hostPath:
            path: /tmp  #The actual path in the worker host
      containers:
        - name: springboot-kubernetes
          volumeMounts:
            - name: host-tmp
              mountPath: /tmp/host  #The application should write to /tmp/host folder because this is where the actual /tmp is mounted at
              
(base) -bash-3.2$ curl http://m2-ess-vm196.mip.storage.hpecorp.net:10005/hostpath/greeting
Hello! Current time is: Mon May 10 15:21:23 GMT 2021

$ curl -X POST -d 'Hello to worker volume'  http://m2-ess-vm196.mip.storage.hpecorp.net:10005/hostpath/post_greetings
Hello+to+worker+volume= saved successfully

$ curl http://m2-ess-vm196.mip.storage.hpecorp.net:10005/hostpath/get_all_greetings
["Hello+to+worker+volume= stored in host:springboot-kubernetes-578f5d7df-7p6xt on Mon May 10 15:28:24 GMT 2021 in file /tmp/host/greetingsFile.txt"]
```
Both the pods in this example were assigned to the same worker. So all the POSTed messages 
go to the same file at the mount path on the worker node 201.
```
[root@m2-e910-201 ~]# hostname
m2-e910-201.mip.storage.hpecorp.net
[root@m2-e910-201 ~]# hostname -i
10.163.169.201
[root@m2-e910-201 ~]# cat /tmp/greetingsFile.txt
Hello+to+worker+volume= stored in host:springboot-kubernetes-578f5d7df-7p6xt on Mon May 10 15:28:24 GMT 2021 in file /tmp/host/greetingsFile.txt
Hello+again+to+worker+volume= stored in host:springboot-kubernetes-578f5d7df-7668b on Mon May 10 15:33:19 GMT 2021 in file /tmp/host/greetingsFile.txt
```
