### Jenkins安装相关插件（"系统管理" -> "管理插件"）

- CloudBees Docker Build and Publish plugin
    > docker插件，在"构建"步骤增加"Docker Build and Publish"，把构建结果Build到docker以及push到registry
- CloudBees Docker Custom Build Environment Plugin
    > docker插件，在"构建环境"步骤增加"Build inside a Docker container"，在构建环境的时候下载docker客户端，在docker中进行项目构建
- docker-build-step
    > docker插件，在"构建"步骤增加"Execute Docker command"，在构建过程中增加docker客户端指令步骤
- GitLab Plugin
    > gitlab插件，在"General"步骤增加"GitLab connection"，源码管理可以调用gitlab
- Gitlab Authentication plugin
    > gitlab插件，可以使用gitlab的api token进行授权
- Gitlab Hook Plugin
    > gitlab插件，在"构建触发器"步骤增加"Build when a change is pushed to GitLab. GitLab CI Service URL: http://localhost:8080/project/XXX"
    
    > 当gitlab代码发生提交的时候，通过gitlab hook主动触发构建 
- Kubernetes plugin
    > kubernetes插件，可以在kubernetes中启动相关pod
- Maven Integration plugin
    > Maven插件，可以增加“构建一个Maven项目”
    > 错误修复：
    > 如果安装了“CloudBees Docker Custom Build Environment Plugin”，在进行maven构建的时候，会出现调用dockerhost连接错误的提示：
    ```
    Established TCP socket on dockerhost:57438
    maven33-agent.jar already up to date
    maven33-interceptor.jar already up to date
    maven3-interceptor-commons.jar already up to date
    [cyberoptic-demo-core-messages] $ /etc/alternatives/java_sdk_1.8.0/bin/java -cp /var/lib/jenkins/slave-node/maven33-agent.jar:/opt/apache-maven-3.3.3/boot/plexus-classworlds-2.5.2.jar:/opt/apache-maven-3.3.3/conf/logging jenkins.maven3.agent.Maven33Main /opt/apache-maven-3.3.3 /var/lib/jenkins/slave-node/slave.jar /var/lib/jenkins/slave-node/maven33-interceptor.jar /var/lib/jenkins/slave-node/maven3-interceptor-commons.jar dockerhost:57438
    ```

    > 那是因为jenkins安装了“CloudBees Docker Custom Build Environment Plugin”，并且发现dockerhost这个主机名能够访问，这个时候，就会把本机当成在docker中运行的slave jenkins，并且尝试连接dockerhost来启动maven。

    > 主要是因为使用了中国移动的CMCC或者热点上网，导致DNS劫持。务必修改Mac系统的dockerd的daemon.json设置：
    ```
    {
      "dns": [
        "114.114.114.114"
      ],
      "registry-mirrors" : [
        "http://3d13f480.m.daocloud.io"
      ]
    }
    ```


### Jenkins中GitLab、Docker、Maven基础配置

- GitLab连接设置（"系统管理" -> "系统设置" -> "GitLab connections"）
    > "Connection name" 设置为 gitlab_cookeem
    
    > "Gitlab host URL" 设置为 http://gitlab
     
    > "Credentials" 需要"Add Credentials"，"Kind" 选择 "GitLab API token"；"API token"对应 Gitlab "User Settings" -> "Account" -> "Private token"
    
    > "Test Connection" 检测GitLab API token能够正常连接

- Docker环境设置（"系统管理" -> "Global Tool Configuration" -> "Docker" -> "Docker安装"）
    > "新增Docker" 新增一个Docker版本的环境变量
    
    > "Name" 设置为 docker_1.13.1；"自动安装" 选择上
    
    > "新增安装" 选择 "Install latest from docker.io"
    
    > "Docker version" 设置为 1.13.1
    
- Docker Builder环境设置，对应docker-build-step插件（"系统管理" -> "系统设置" -> "Docker Builder"）
    > "Docker URL" 设置为 tcp://docker:2375
    
    > "Test Connection" 检测连接是否正常
    
- Maven环境设置（"系统管理" -> "Global Tool Configuration" -> "Maven" -> "Maven安装"）
    > "新增Maven" 新增一个Maven版本的环境变量
    
    > "别名" 设置为 maven3.5.0；"自动安装" 选择上
    
    > "新增安装" 选择 "Install from Apache"
    
    > "Version" 选择 Maven的版本

- JDK环境设置（"系统管理" -> "Global Tool Configuration" -> "JDK" -> "JDK安装"）
    > "新增JDK" 新增一个JDK版本的环境变量
    
    > "别名" 设置为 jdk8u131；"自动安装" 选择上
    
    > "新增安装" 选择 "从java.sun.com安装"
    
    > "版本" 选择JDK的版本

### Jenkins中新建项目，实现Maven项目通过GitLab进行源码管理和自动打包到Docker

- "新建" -> "构建一个maven项目"

- "General"设置
    > "项目名称" 设置为 CookIM
    
    > "GitLab connection" 选择 gitlab_cookeem（对应"系统管理" -> "系统设置" -> "GitLab connections"）

- "源码管理"设置
    > "Git" -> "Repositories" -> "Repository URL" 设置为 http://gitlab/cookeem/CookIM
    
    > "Git" -> "Repositories" -> "Credentials" -> "Add Credentials"，"Kind" 选择 "Username with password"，"Username" 设置为 cookeem@qq.com，"Password" 设置为对应GitLab账号密码

- "构建触发器"设置
    > "Build when a change is pushed to GitLab. GitLab CI Service URL: http://localhost:8080/project/CookIM" 该项选择
    
    > "Build when a change is pushed to GitLab." -> "高级" -> "Secret token" -> "Generate" 创建Jenkins token
    
    > 打开GitLab界面，"Projects" -> "cookeem/CookIM" -> "Settings" -> "Integrations"，"URL" 设置为 http://jenkins:8080/project/CookIM（对应Jenkins的"GitLab CI Service URL"），"Secret Token" 设置为对应Jenkins的"Secret token"。创建WebHook后进行测试，就会触发自动构建

- "构建环境"设置
    > "Add timestamps to the Console Output" 选择上
    
- "Pre Steps"设置
    > "新增构建步骤" -> "Execute shell"，执行以下构建脚本
```
printenv
```

- "Build"设置
    > "Goals and options" 设置为 clean install
    > 点击高级
    > "Settings file" 选择 "Settings file in filesystem"
    > "File path" 设置为 /var/jenkins_home/maven/settings.xml （注意，务必设置settings.xml的mirrors设置指向nexus）

- "Post Steps"设置
    > "Add post-build step" -> "Execute shell"，执行以下构建脚本
```
# 设置DOCKER_HOME
export MY_DOCKER_HOME=/var/jenkins_home/tools/org.jenkinsci.plugins.docker.commons.tools.DockerTool/docker_1.13.1
export PATH=$PATH:$MY_DOCKER_HOME/bin
export DOCKER_HOST=tcp://ci-docker:2375

# 设置版本信息
export APP_VERSION_NAME=`cat VERSION`

# 把文件复制到项目目录
mv target/cookim-${APP_VERSION_NAME}-allinone.jar cookim.jar

# 构建docker镜像
docker build -t k8s-registry:5000/cookeem/cookim:$APP_VERSION_NAME -f Dockerfile_k8s .

# 把docker镜像推送到k8s-registry:5000
docker push k8s-registry:5000/cookeem/cookim:$APP_VERSION_NAME

# 使用kubectl拉起镜像
kubectl apply -f kubernetes/cookim.yaml
```
    
- "保存"项目

- GitLab中进行push，触发Jenkins进行Maven项目构建，完成构建后，把编译包build成docker镜像，并且把镜像push到docker registry

- 源码的根目录需要创建Dockerfile，用于"CloudBees Docker Build and Publish plugin"进行自动构建docker镜像

- 在jenkins容器中测试CookIM是否启动正常
    ```
        docker exec -ti jenkins bash
        curl docker:8081/user/haijian/ok
        exit
    ```

- 在docker容器中测试CookIM是否启动正常，检测logs中的App Version
    ```
        docker exec -ti docker ash
        docker images
        docker ps
        docker logs CookIM
        exit
    ```

- 关闭服务，注意，如果只是stop再up，docker容器启动会出现异常
    ```
        docker-compose stop && docker-compose rm -f
    ```
