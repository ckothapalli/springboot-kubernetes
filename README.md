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

This ClusterIP can be seen in the /etc/resolv.conf on any of the pods. This is how pods can find other services thorugh their names.
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

We can also see the how the DNS record is for this service. The serviceName.namespace.svc.clusterName is the fully qualified domain name of the service.

```
/ # nslookup nginx-svc-np
Server:		10.96.0.10
Address:	10.96.0.10:53

Name:	nginx-svc-np.default.svc.cluster.local
Address: 10.99.167.197
```
