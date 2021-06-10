# Deploy nginx pod

```
kubectl run nginx --image=nginx

vagrant@kubemaster2:~$ kubectl get po -o wide --show-labels
NAME    READY   STATUS    RESTARTS   AGE   IP          NODE         NOMINATED NODE   READINESS GATES   LABELS
nginx   1/1     Running   0          61s   10.36.0.1   kubenode22   <none>           <none>            run=nginx
```
The label ```run=nginx``` is given by default.
The pod is assigned IP 10.36.0.1

Login to the pod and test nginx. We confirm that the nginx is running on port 80 in the pod
```
vagrant@kubemaster2:~$ kubectl exec -it nginx -- /bin/sh
# curl http://localhost:80
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
...
<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```
# Deploy a NodePort type service and expose the above nginx pod through it
See that we set the targetPort to 80, which is the port on which nginx is running in the pod.
We did not explicitly specify the nodePort (the port for a node through which the service is exposed)
```
vagrant@kubemaster2:~$ cat serv.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-svc-np
spec:
  type: NodePort
  ports:
  - port: 80
    protocol: TCP
    targetPort: 80
  selector:
    run: nginx

kubectl apply -f serv.yaml

vagrant@kubemaster2:~$ kubectl get svc -o wide
NAME           TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE    SELECTOR
kubernetes     ClusterIP   10.96.0.1       <none>        443/TCP        7d8h   <none>
nginx-svc-np   NodePort    10.99.167.197   <none>        80:32502/TCP   43m    run=nginx


vagrant@kubemaster2:~$ kubectl describe svc nginx-svc-np
Name:                     nginx-svc-np
Namespace:                default
Labels:                   <none>
Annotations:              <none>
Selector:                 run=nginx
Type:                     NodePort
IP Family Policy:         SingleStack
IP Families:              IPv4
IP:                       10.99.167.197
IPs:                      10.99.167.197
Port:                     <unset>  80/TCP
TargetPort:               80/TCP
NodePort:                 <unset>  32502/TCP
Endpoints:                10.36.0.1:80
```

Note the selector, which is set to the label of the nginx pod deployed above.
See the Endpoints, through which we can access the ngix.
```
vagrant@kubemaster2:~$ curl http://10.36.0.1:80
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
...
<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

We can access through the NodePort through any of the nodes as well.

```
vagrant@kubemaster2:~$ kubectl get no
NAME          STATUS   ROLES                  AGE    VERSION
kubemaster2   Ready    control-plane,master   7d8h   v1.21.1
kubenode21    Ready    <none>                 7d8h   v1.21.1
kubenode22    Ready    <none>                 7d8h   v1.21.1

vagrant@kubemaster2:~$ curl http://kubenode22:32502
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

# DNS

See the DNS exposed as a service kube-dns on ClusterIP 10.96.0.10.  
```
vagrant@kubemaster2:~$ kubectl get svc -A
NAMESPACE     NAME           TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                  AGE
default       kubernetes     ClusterIP   10.96.0.1       <none>        443/TCP                  7d8h
default       nginx-svc-np   NodePort    10.99.167.197   <none>        80:32502/TCP             73m
kube-system   kube-dns       ClusterIP   10.96.0.10      <none>        53/UDP,53/TCP,9153/TCP   7d8h
```

This ClusterIP of the kube-dns service can be seen in the /etc/resolv.conf on any of the pods. This is how pods can find other services thorugh their names.
Below, we can see the DNS record in the nginx pod itself.
```
vagrant@kubemaster2:~$ kubectl exec -it nginx -- /bin/sh
# cat /etc/resolv.conf
nameserver 10.96.0.10
search default.svc.cluster.local svc.cluster.local cluster.local hitronhub.home
options ndots:5
```
From the nginx pod itself, we can access the nginx service using DNS like below.
```
# curl http://nginx-svc-np
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

Try the same from another test pod busybox. Here we create the busybox pod that waits for 1000 seconds. Then we log into the busybox and access the service using the DNS name.
```
kubectl run busybox --image=busybox --command sleep 1000

kubectl exec -it busybox -- /bin/sh

/ # wget http://nginx-svc-np
Connecting to nginx-svc-np (10.99.167.197:80)
saving to 'index.html'
index.html           100% |************************************************************************************************************|   612  0:00:00 ETA
'index.html' saved
/ # cat index.html
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>

</body>
</html>
```

We can also see the how the DNS record is for this service. The serviceName.namespace.svc.clusterRootDomain is the fully qualified domain name of the service.

```
/ # nslookup nginx-svc-np
Server:		10.96.0.10
Address:	10.96.0.10:53

Name:	nginx-svc-np.default.svc.cluster.local
Address: 10.99.167.197
```

The DNS entry is set up in each pod as they are created by the Kubelet. We can see this in the kubelet configuration file.
```
root     10257     1  2 Jun02 ?        02:53:42 /usr/bin/kubelet --bootstrap-kubeconfig=/etc/kubernetes/bootstrap-kubelet.conf --kubeconfig=/etc/kubernetes/kubelet.conf --config=/var/lib/kubelet/config.yaml --network-plugin=cni --pod-infra-container-image=k8s.gcr.io/pause:3.4.1
```

```
vagrant@kubemaster2:~$ sudo cat /var/lib/kubelet/config.yaml
apiVersion: kubelet.config.k8s.io/v1beta1
authentication:
  anonymous:
    enabled: false
  webhook:
    cacheTTL: 0s
    enabled: true
  x509:
    clientCAFile: /etc/kubernetes/pki/ca.crt
authorization:
  mode: Webhook
  webhook:
    cacheAuthorizedTTL: 0s
    cacheUnauthorizedTTL: 0s
cgroupDriver: systemd
clusterDNS:
- 10.96.0.10
clusterDomain: cluster.local
```
# How the DNS server itself is configured
See the core-dns pods and how they are exposed through a kube-dns service. Check the pod label ```k8s-app=kube-dns``` which is used as the selector for the service.
```
vagrant@kubemaster2:~$ kubectl get po -n kube-system -o wide --show-labels | grep dns
coredns-558bd4d5db-w62gj              1/1     Running   0          7d9h   10.32.0.3      kubemaster2   <none>           <none>            k8s-app=kube-dns,pod-template-hash=558bd4d5db
coredns-558bd4d5db-wv8p2              1/1     Running   0          7d9h   10.32.0.2      kubemaster2   <none>           <none>            k8s-app=kube-dns,pod-template-hash=558bd4d5db
vagrant@kubemaster2:~$
vagrant@kubemaster2:~$ kubectl get svc -n kube-system -o wide
NAME       TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)                  AGE    SELECTOR
kube-dns   ClusterIP   10.96.0.10   <none>        53/UDP,53/TCP,9153/TCP   7d9h   k8s-app=kube-dns
```

Describe the core-dns pod to view the Corefile defined as a configmap volume.
```
vagrant@kubemaster2:~$ kubectl describe po coredns-558bd4d5db-w62gj -n kube-system
Name:                 coredns-558bd4d5db-w62gj
Namespace:            kube-system
Node:                 kubemaster2/192.168.46.2
Labels:               k8s-app=kube-dns
                      pod-template-hash=558bd4d5db
Status:               Running
IP:                   10.32.0.3
IPs:
  IP:           10.32.0.3
Controlled By:  ReplicaSet/coredns-558bd4d5db
Containers:
  coredns:
    Container ID:  docker://8c49c49b37af5891815e4439653abdee3c9f3e3e7b901e78bcedbf690fe4302d
    Image:         k8s.gcr.io/coredns/coredns:v1.8.0
    Image ID:      docker-pullable://k8s.gcr.io/coredns/coredns@sha256:cc8fb77bc2a0541949d1d9320a641b82fd392b0d3d8145469ca4709ae769980e
    Ports:         53/UDP, 53/TCP, 9153/TCP
    Host Ports:    0/UDP, 0/TCP, 0/TCP
    Args:
      -conf
      /etc/coredns/Corefile
    State:          Running
      Started:      Sun, 30 May 2021 13:54:45 +0000
    Ready:          True
    Restart Count:  0
    Limits:
      memory:  170Mi
    Requests:
      cpu:        100m
      memory:     70Mi
    Liveness:     http-get http://:8080/health delay=60s timeout=5s period=10s #success=1 #failure=5
    Readiness:    http-get http://:8181/ready delay=0s timeout=1s period=10s #success=1 #failure=3
    Environment:  <none>
    Mounts:
      /etc/coredns from config-volume (ro)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-qlhnp (ro)

Volumes:
  config-volume:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      coredns
    Optional:  false
  kube-api-access-qlhnp:
    Type:                    Projected (a volume that contains injected data from multiple sources)
    TokenExpirationSeconds:  3607
    ConfigMapName:           kube-root-ca.crt
    ConfigMapOptional:       <nil>
    DownwardAPI:             true
```

See the details of the ConfigMap
```
vagrant@kubemaster2:~$ kubectl describe cm coredns -n kube-system
Name:         coredns
Namespace:    kube-system
Labels:       <none>
Annotations:  <none>

Data
====
Corefile:
----
.:53 {
    errors
    health {
       lameduck 5s
    }
    ready
    kubernetes cluster.local in-addr.arpa ip6.arpa {
       pods insecure
       fallthrough in-addr.arpa ip6.arpa
       ttl 30
    }
    prometheus :9153
    forward . /etc/resolv.conf {
       max_concurrent 1000
    }
    cache 30
    loop
    reload
    loadbalance
}
```
