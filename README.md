# Guestbook Microservice

**Guestbook Messenger App:** **https://istio.skycomposer.net**

**Kibana Dashboard:** **https://istio.skycomposer.net/kibana**

**Grafana Dashboard:** **https://istio.skycomposer.net/grafana**

**Kiali Management Console:** **https://istio.skycomposer.net/kiali**

**Jaeger Distributed Tracing:** **https://istio.skycomposer.net/jaeger**

# Microservices Monitoring on AWS with Terraform, K3S Kubernetes Cluster, Istio Gateway, Fluentd and Kibana Logging, Jaeger Distributed Tracing, Kiali Management Console and Grafana Monitoring Dashboard with Prometheus Datasource:

## Step 01 - Setup terraform account on AWS:
#### Skip to Step 02, if you already have working Terraform account with all permissions

#### Setting Up an AWS Operations Account

 - Log in to your AWS management console with your root user credentials. Once you’ve logged in, you should be presented with a list of AWS services. Find and select the IAM service.

 - Select the Users link from the IAM navigation menu on the lefthand side of the screen. Click the Add user button to start the IAM user creation process

 - Enter **ops-account** in the User name field. We also want to use this account to acccess the CLI and API, so select “Programmatic access” as the AWS “Access type”

-  Select “Attach existing policies directly” from the set of options at the top. Search for a policy called IAMFullAccess and select it by ticking its checkbox

- If everything looks OK to you, click the Create user button.

#### Access key and secret key

- Before we do anything else, we’ll need to make a note of our new user’s keys. Click the Show link and copy and paste both the “**Access key ID**” and the “**Secret access key**” into a temporary file. We’ll use both of these later. Be careful with this key material as it will give whoever has it an opportunity to create resources in your AWS environment — at your expense.

- Make sure you take note of the access key ID and the secret access key that were generated before you leave this screen. You’ll need them later.

- We have now created a user called **ops-account** with permission to make IAM changes. 

#### Configure the AWS CLI

```
$ aws configure
AWS Access Key ID [****************AMCK]: AMIB3IIUDHKPENIBWUVGR
AWS Secret Access Key [****************t+ND]: /xd5QWmsqRsM1Lj4ISUmKoqV7/...
Default region name [None]: eu-west-2
Default output format [None]: json
```

You can test that you’ve configured the CLI correctly by listing the user accounts that have been created. Run the iam list-users command to test your setup:

```
$ aws iam list-users
{
    "Users": [
        {
            "Path": "/",
            "UserName": "admin",
            "UserId": "AYURIGDYE7PXW3QCYYEWM",
            "Arn": "arn:aws:iam::842218941332:user/admin",
            "CreateDate": "2019-03-21T14:01:03+00:00"
        },
        {
            "Path": "/",
            "UserName": "ops-account",
            "UserId": "AYUR4IGBHKZTE3YVBO2OB",
            "Arn": "arn:aws:iam::842218941332:user/ops-account",
            "CreateDate": "2020-07-06T15:15:31+00:00"
        }
    ]
}
```

If you’ve done everything correctly, you should see a list of your AWS user accounts. That indicates that AWS CLI is working properly and has access to your instance. Now, we can set up the permissions our operations account will need.

#### Setting Up AWS Permissions

The first thing we’ll do is make the ops-account user part of a new group called Ops-Accounts. That way we’ll be able to assign new users to the group if we want them to have the same permissions. Use the following command to create a new group called Ops-Accounts:

```
$ aws iam create-group --group-name Ops-Accounts
```

If this is successful, the AWS CLI will display the group that has been created:

```
{

    "Group": {
        "Path": "/",
        "GroupName": "Ops-Accounts",
        "GroupId": "AGPA4IGBHKZTGWGQWW67X",
        "Arn": "arn:aws:iam::842218941332:group/Ops-Accounts",
        "CreateDate": "2020-07-06T15:29:14+00:00"
    }
}
```

Now, we just need to add our user to the new group. Use the following command to do that:

```
$ aws iam add-user-to-group --user-name ops-account --group-name Ops-Accounts
````

If it works, you’ll get no response from the CLI.

Next, we need to attach a set of permissions to our Ops-Account group. 

```
$ aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/IAMFullAccess &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonEC2FullAccess &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryFullAccess &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonEKSClusterPolicy &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonEKSServicePolicy &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonVPCFullAccess &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonRoute53FullAccess &&\
aws iam attach-group-policy --group-name Ops-Accounts\
 --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

```

With the new policy created, all that’s left is to attach it to our user group. Run the following command, replacing the token we’ve called {YOUR_POLICY_ARN} with the ARN from your policy:

```
$ aws iam attach-group-policy --group-name Ops-Accounts \
   --policy-arn {YOUR_POLICY_ARN}
```

#### Creating an S3 Backend for Terraform

- If you are hosting your bucket in the us-east-1 region, use the following command:

```
$ aws s3api create-bucket --bucket {YOUR_S3_BUCKET_NAME} --region us-east-1
```

- If you are hosting the s3 bucket in a region other than us-east-1, use the following command:

```
$ aws s3api create-bucket --bucket {YOUR_S3_BUCKET_NAME} \
> --region {YOUR_AWS_REGION} --create-bucket-configuration \
> LocationConstraint={YOUR_AWS_REGION}
```




## Step-02: Setup your local Terraform Environment:

- create private and public SSH Keys. Terraform will use them to run scripts on your EC2 instances:

```
    ssh-keygen -t rsa
````

- go to "**terraform**" folder of this github repository

- create file "**terraform.auto.tfvars**" in "**terraform**" folder:

```
access_ip = "0.0.0.0/0"
public_key_path = "/Users/dddd/.ssh/keymtc.pub"
private_key_path = "/Users/dddd/.ssh/keymtc"
certificate_arn = "arn:aws:acm:ddddddddddddddddddddf"
shared_credentials_file = "/Users/dddd/.aws/credentials"
profile_account = "ops-account"
```

- make sure you provide correct path for "**public_key_path**" and "**private_key_path**"

- make sure you provide correct "**certifcate_arn**" for your AWS certificate, registered to your domain. You need to register your domain and create certificate for your domain in AWS

- make sure you provide correct path for your AWS credentials ("**shared_credentials_file**" variable)

- make sure you provide correct aws profile account name for "**profile_account**" variable (it should be "**ops-account**", if you followed instructions in "**Step 01**")

- replace "**skycomposer-istio*" in "**backends.tf**" with the name of your S3 bucket, created in "**Step 01**"

- replace "**us-west-2**" in "**backends.tf**" with the name of your **AWS region**

- replace "**us-west-2**" in "**variables.tf**" with the name of your **AWS region**


- run the following commands:

```
terraform init

terraform validate

terraform apply --auto-approve

``` 

- terraform will automatically create KUBECONFIG file, so you can switch to your created K3S Kubernetes cluster by using
```
export KUBECONFIG=./ks3/k3s.yaml
``` 


## Step-03: Register your domain "test.com", create AWS Certificate for "*.test.com" and create Hosted Zone CNAME Record with DNS Name of your Load Balancer:

- go to "**EC2 -> Load Balancers**" in your AWS Console

- copy DNS name of your load balancer

- go to "**Route53 -> Hosted Zones -> Your Hosted Zone -> Create Record**"

- let's assume that the name of your domain is "**test.com**" and "**DNS name**" of your LoadBalancer is "**mtc-loadbalancer.com**"

- create "**CNAME**" record with the name "**istio.test.com**" and the value "**mtc-loadbalancer.com**"

- let's assume that the name of your "**CNAME**" record is "**istio.test.com**" 

- let's assume that "**DNS name**" of your Load Balancer is "**mtc-loadbalancer.com**"

- let's assume that you correctly registered your domain, created hosted zone, registered AWS SSL Certificate for your domain "***.test.com**" and created "**CNAME**" record with the name "**istio.test.com**" and the value "**mtc-loadbalancer.com**"



## Step-04: Deploy Istio Service Mesh to AWS K3S Cluster:

- install "**istioctl**" command-line tool (see installation options for your operating system, for example, in MacOS you can use "**homebrew**")

- go to "**terraform**" directory and run the following commands:

``` 
export KUBECONFIG=./ks3/k3s.yaml

istioctl install --set profile=demo -y

kubectl label namespace default istio-injection=enabled
``` 

From now on, every pod in your default namespace will be injected with Istio Sidecar Proxy


## Step-05: Deploy "Guestbook" Microservice and Istio Gateway to AWS K3S Cluster:

- go to "**k3s**" folder of this github repository

- Edit "**301-istio-gateway.yaml**": replace "**istio.skycomposer.net**" with the name of your sub-domain ("**istio.test.com**", for example)

- Edit "**302-istio-virtualservices.yaml**": replace "**istio.skycomposer.net**" with the name of your sub-domain ("**istio.test.com**", for example)

- go back to "**terraform**" directory and run the following commands:

``` 
export KUBECONFIG=./ks3/k3s.yaml

kubectl apply -f ../k3s

kubectl get pods
``` 

Make sure that all pods in default namespace have 2 containers


## Step-06: Deploy Kibana, Grafana, Kiali and Jaeger to AWS K3S Cluster:

- go to "**k3s-dashboard**" folder of this github repository

- Edit "**grafana.yaml**": replace the value of "**GF_SERVER_ROOT_URL**" property: "**https://istio.skycomposer.net/grafana**" with correspondent url of your domain ("**https://istio.test.com/grafana**", for example)

- Edit "**kiali.yaml**": replace the value of "**grafana: url**" property: "**https://istio.skycomposer.net/grafana/**" with correspondent url of your domain ("**https://istio.test.com/grafana**", for example)

- Edit "**kiali.yaml**": replace the value of "**tracing: url**" property: "**https://istio.skycomposer.net/jaeger**" with correspondent url of your domain ("**https://istio.test.com/jaeger**", for example)

- go back to "**terraform**" directory and run the following commands:

``` 
export KUBECONFIG=./ks3/k3s.yaml

kubectl apply -f ../k3s-logging

kubectl apply -f ../k3s-dashboard
``` 

If you see any errors with command "**kubectl apply -f ../k3s-dashboard**", try the command again


## Step-07: Test Kibana Logging Dashboard:

- go to "**https://istio.test.com**"
- you should see successfully loaded "**Guestbook**" page
- Submit several messages
- go to "**https://istio.test.com/kibana**"
- Configure Kibana Dashboard (Create Index Pattern, Discover Logs, Create Visualizations, Create Dashboard)
- make sure that filter ```kubernetes.pod_name : guestbook and log : *guestbook*``` returns logs related to your last activity on "**Guestbook**" page
- make sure that filter ```kubernetes.pod_name : guestbook and log : *200*``` shows successfull HTTP Responses



## Step-08: Test Grafana, Kiali and Jaeger Dashboards:

- go to "**https://istio.test.com/grafana**"
- you should see successfully loaded "**Grafana**" dashboard
- go to "**https://istio.test.com/kiali**"
- you should see successfully loaded "**Kiali**" dashboard
- go to "**https://istio.test.com/jaeger**"
- you should see successfully loaded "**Jaeger**" dashboard




### Congratulations! You sucessfully deployed Kibana Logging Dashboard, Grafana Monitoring Dashboard, Kiali Mangament Console and Jaeger Distributed Tracing on AWS with Terraform and K3S!
- ### Now you can deploy your own docker containers to this cluster with minimal costs from AWS!
- ### You significantly reduced your AWS bills by removing AWS EKS and NAT gateway!
- #### You also implemented Istio Gateway, which acts as a Gateway API for your microservices
- #### Now you can add any number of microservices to your K3S Kubernetes Cluster and use only one Istio Gateway for all these microservices 

- ### You successfully deployed Guestbook Messenger App, which can be used to easily generate any number of logs for Kibana and Istio Dashboards
- #### You successfully deployed and exposed Kibana Logging Dashboard, Grafana Monitoring Dashboard, Kiali Management Console and Jaeger Distributed Tracing
- #### You externally exposed all these tools on your own registerd domain, with secured HTTPS connection
- #### Now you can share these links to provide Centralized Logging and Monitoring for your portfolio applications


## Step-09: Clean-Up:

```
terraform destroy --auto-approve  
```
