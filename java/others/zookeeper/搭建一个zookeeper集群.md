## ZooKeeper基础
ZooKeeper是一个高性能的分布式应用协调服务。它通过简单的接口提供像命名、配置管理、同步和分组等通用服务，我们不需要重复造轮子。
用它我们可以很简单的实现一致性、分组管理、选主和存在协议，也可以自己实现特殊的需求。    
分布式系统的服务协调是一个臭名昭著的难以正确实现的问题，很容易出现竞争和死锁这样的问题。ZK的动机就来源于把分布式应用从
自己实现服务协调的困境中解脱出来。
### 理论基础
#### ACID
所谓ACID，狭义上是指对传统数据库事务必备特性的总结。包括:    
**原子性(Atomicity)**    
&ensp;&ensp;原子性是指一个事务就是一个数据库操作的基本单元，不管事务包含多少具体操作，这个事务的执行结果
只有失败和成功两种。如果事务中的某一步操作失败，那么这个事务中已经执行的操作会全部回滚，并返回失败。
只有事务中的所有操作都成功并且提交成功，事务才会返回成功。  
**一致性(Consistency)**    
&ensp;&ensp;所谓的一致状态是指关联数据之间的关联关系都是完整和正确的。而数据库事务的一致性是指任何一个
数据库事务都只能使数据从一个一致性状态进入另一个一致性状态。    
**隔离性(Isolation)**    
&ensp;&ensp;隔离性是指事务之间会不会干扰，或者说一个事务的中间状态对其他事务的可见性。
在标准SQL规范中定义了四种事务的隔离级别：    
&ensp;&ensp;读未提交(Read Uncommited)    
&ensp;&ensp;&ensp;&ensp;一个事务的中间状态对其他事物是完全可见的。    
&ensp;&ensp;读已提交(Read Commited)    
&ensp;&ensp;&ensp;&ensp;其他事务只能看到一个事务的最终状态。在一个事务的执行过程中有可能读取到其他多个事务的结果，也就是说，在一个事务的
执行过程中读取到的数据会发生变化。  
&ensp;&ensp;可重复读(Repeatable Read)    
&ensp;&ensp;&ensp;&ensp;在一个事务的进行过程中，他所读取到的数据是不变的，不管其他事务在进行什么操作。   
&ensp;&ensp;串行化(Serialized)    
&ensp;&ensp;&ensp;&ensp;所有事务串行处理。

| 隔离级别          |脏读     |不可重复读 |幻读   |
| --------          |-----  |----       |----   |
| Read Uncommited   |√      |√          |√       |
| Read Commited     |×      |√          |√       |
| Repeatable Read   |×      |×          |√       |
| Serialized        |×      |×          |×       |

**持久性(Durability)**    
&ensp;&ensp;持久性是指数据库的数据一旦提交之后就是持久化并且不会自己发生改变的。

#### CAP理论
所谓CAP理论是指，一个分布式系统不可能同时满足一致性(C:Consistency)、可用性(A:Availability)和
分区容错性(P:Partition tolerance)这三个基本要素，最多同时满足其中的两项。   
**一致性(C:Consistency)**    
分布式系统的数据一致性是指在分布式系统中，同一份数据在集群所有副本中是一致的，对数据的写会同步到所有副本中，
对数据的读无论路由到那一个副本上得到的结果都是一样的。满足这样的条件的系统被称作是具有强一致性或者严格一致性的。    
**可用性(A:Availability)**    
即系统在可接受的时间内对请求返回明确的结果。    
**分区容错性(P:Partition Tolerance)**    
当发生系统分区，或者称脑裂的时候，系统仍然能够提供可用和一致性的服务。  
事实上，对于一个分布式系统来说，分区容错性是必备的，因此我们需要关心的是如何权衡一致性和可用性。因此出现了BASE理论。

#### BASE理论
BASE理论是在CAP理论的基础上，根据大规模互联网系统分布式实践经验权衡一致性和可用性得出的经验理论。是
基本可用性(Basically Available)，软状态(Soft state)和最终一致性(Eventual consistency)的缩写，其核心思想是：
即使无法做到强一致性(Strong Consistency)，但是应用可以根据业务特点采用适当的方式实现最终一致性(Eventual Consistency)。   
**基本可用性(Basically Available)**  
基本可用性是指在分布式系统遭遇不可预知的故障时，可以损失部分可用性，比如响应速度变慢或者切换降级方案等，但这绝不能
和不可用状态等同。  
**软状态(Soft state)**  
软状态或者弱状态，是指允许事务的中间状态对外可见，我们可以类比数据库不同的事务隔离级别来理解。比如允许在分布式系统中，
不同节点数据同步存在延时。  
**最终一致性(Eventual consistency)**  
最终一致性强调分布式系统中的同一份数据的所有副本，经过一段时间的同步后能够最终达到一个一致的状态，
在这段时间里允许数据的不一致情况存在。在实践中，这种最终一致性可以有如下一些实现方式：   
因果一致性(Casual Consistency)：即如果进程A在更新了某些数据之后通知了进程B，那么B对这些数据的读写都应该基于A的更新结果。   
读己之所写(Read Your Writes)：即某进程更新了某些数据之后，它自己总能实时看到更新的结果。   
会话一致性(Session Consistency)：即在同一个有效会话中实现读己之所写。  
单调读一致性(Monotonic Read Consistency)：如果一个进程从系统中读取到了某些数据，那么在之后的时间点中就
不会读取到这些数据更老的版本。  
单调写一致性(Monotonic Write Consistency)：对同一数据的写操作顺序执行。
### 搭建一个5节点的ZooKeeper集群
    环境和版本信息
        1.VMware® Workstation 12 Pro - 12.5.2 build-4638234
        2.CentOS Linux release 7.3.1611 (Core) 
            192.168.137.27
            192.168.137.61
            192.168.137.53
            192.168.137.117
            192.168.137.156            
        3.java version "1.8.0_131"
        4.zookeeper-3.4.9
1.解压安装复制配置文件 - 略    
2.编辑配置文件zoo.cfg，添加五个节点的配置信息

    # The number of milliseconds of each tick
    tickTime=2000
    # The number of ticks that the initial
    # synchronization phase can take
    initLimit=10
    # The number of ticks that can pass between
    # sending a request and getting an acknowledgement
    syncLimit=5
    # the directory where the snapshot is stored.
    dataDir=/usr/zk/data
    # the port at which the clients will connect
    clientPort=2181
    # Peers use the former port to connect to other peers. Such a connection is necessary so that peers 
    # can communicate, for example, to agree upon the order of updates. More specifically, a ZooKeeper server 
    # uses this port to connect followers to the leader. When a new leader arises, a follower opens a TCP 
    # connection to the leader using this port. Because the default leader election also uses TCP, we currently 
    # require another port for leader election. This is the second port in the server entry.
    server.1=192.168.137.53:2888:3888
    server.2=192.168.137.61:2888:3888
    server.3=192.168.137.27:2888:3888
    server.4=192.168.137.117:2888:3888
    server.5=192.168.137.156:2888:3888
3.配置文件及分发    
3.1.配置ssh免密码登录   
在实际生产环境中，zk节点之间可以互相配置ssh免密码登录，方便运维。   
3.2.分发配置文件，例如在server.3上执行以下命令，会将我们配置好的zoo.cfg分发到server.1上    
    
    scp /usr/zk/zookeeper-3.4.9/conf/zoo.cfg root@192.168.137.53:/usr/zk/zookeeper-3.4.9/conf/zoo.cfg

3.3.编写myid    
我们注意到在zoo.cfg中我们指定了我们的5台ZooKeeper机器实例，在server.X中，数字X就代表每个实例的ID，这个ID我们必须要
写在我们配置的dataDir中，并命名为myid，其内容就是这个数字X。   
4.启/停集群    
我们可以使用终端工具secureCRT，同时连接我们的5台机器，然后在交互窗口中把命令发送到所有会话，同时启动五个zk节点

    ./zkServer.sh start ../conf/zoo.cfg
通过zkServer命令也可以关闭zk节点

    ./zkServer.sh stop
我们也可以通过如下脚本关闭zk

    jps | grep -v Jps | awk '{print $1}' | xargs kill -9
5.trouble shoot   
也许你需要关闭防火墙```systemctl stop firewalld.service```(或者放行端口)
