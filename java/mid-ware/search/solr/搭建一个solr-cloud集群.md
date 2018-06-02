    环境说明:
    1.VMware® Workstation 12 Pro - 12.5.2 build-4638234
    2.CentOS Linux release 7.3.1611 (Core) 
    3.java version "1.8.0_131"
      Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
      Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
    4.zookeeper-3.4.9
    5.solr-5.5.2
    
### Lucence基础课 // TODO
### SolrCloud简介
从solr4.0版本开始，solr提供了对集群的支持，这就是我们说的SolrCloud。

    SolrCloud is designed to provide a highly available, fault tolerant environment for distributing your indexed
    content and query requests across multiple servers. It's a system in which data is organized into multiple pieces,
    or shards, that can be hosted on multiple machines, with replicas providing redundancy for both scalability and
    fault tolerance, and a ZooKeeper server that helps manage the overall structure so that both indexing and search
    requests can be routed properly.
        1.Central configuration for the entire cluster
        2.Automatic load balancing and fail-over for queries
        3.ZooKeeper integration for cluster coordination and configuration
    
                                                                                    -- Apache Solr Reference Guide 5.2

#### 基本概念
##### core/collection/replica/shard
    On a single node, Solr has a core that is essentially a single index. If you want multiple indexes, you create
    multiple cores. With SolrCloud, a single index can span multiple Solr instances. This means that a single index
    can be made up of multiple cores on different machines.    
    The cores that make up one logical index are called a collection. A collection is a essentially a single index that
    can span many cores, both for index scaling as well as redundancy. If, for instance, you wanted to move your
    two-core Solr setup to SolrCloud, you would have 2 collections, each made up of multiple individual cores.    
    In SolrCloud you can have multiple collections. Collections can be divided into shards. Each shard can exist in
    multiple copies; these copies of the same shard are called replicas. One of the replicas within a shard is the lead
    er, designated by a leader-election process. Each replica is a physical index, so one replica corresponds to one
    core.    
    It is important to understand the distinction between a core and a collection. In classic single node Solr, a core is
    basically equivalent to a collection in that it presents one logical index. In SolrCloud, the cores on multiple nodes
    form a collection. This is still just one logical index, but multiple cores host different shards of the full 
    collection. So a core encapsulates a single physical index on an instance. A collection is a combination of all of 
    the cores that together provide a logical index that is distributed across many nodes.

在Solr的单机环境下，一个core就是一个index，扩展到SolrCloud，一个core就是SolrCloud集群中的一个Solr实例中的一个index，
大多数情况下是因为index数据量过大或者业务原因，此时这个core只包含了这个index的一部分数据。组成一个逻辑上完整的index的所有
core被称作一个collection。所以从功能角度上来说，单机环境下的core与集群环境下的collection是等同的，但是从物理角度来说，
无论单机还是集群环境，core的概念是相同的。从备份和冗余角度来讲，collection又可以分为若干shard，每个shard都会有若干份副本，
或者称为replica，replica有主从之分，这个主从关系由zookeeper维护。从物理上来讲，一个replica就是一个core。
##### Nodes, Cores, Clusters and Leaders
###### Nodes and Cores
    In SolrCloud, a node is Java Virtual Machine instance running Solr, commonly called a server. Each Solr core
    can also be considered a node. Any node can contain both an instance of Solr and various kinds of data.    
    A Solr core is basically an index of the text and fields found in documents. A single Solr instance can contain
    multiple "cores", which are separate from each other based on local criteria. 
    When you start a new core in SolrCloud mode, it registers itself with ZooKeeper. This involves creating an
    Ephemeral node that will go away if the Solr instance goes down, as well as registering information about the
    core and how to contact it (such as the base Solr URL, core name, etc). Smart clients and nodes in the cluster
    can use this information to determine who they need to talk to in order to fulfill a request.
     
Node就是SolrCloud中的一个Solr服务实例，在生产环境中，当我们针对一个索引谈论这个概念时，Node坍缩为一个Solr实例上的一个core。
Core就是我们从所谓的document读取并索引的text和field数据。当我们在SolrCloud中建立一个新的core，它会将自己注册到zookeeper
并提供自己的信息接受其管理。
###### Clusters
    A cluster is set of Solr nodes managed by ZooKeeper as a single unit. When you have a cluster, you can always
    make requests to the cluster and if the request is acknowledged, you can be sure that it will be managed as a
    unit and be durable, i.e., you won't lose data. Updates can be seen right after they are made and the cluster can
    be expanded or contracted. A cluster is created as soon as you have more than one Solr instance registered with 
    ZooKeeper.

Cluster，即集群，就是注册到同一个zookeeper并且接受其管理的若干Solr实例。一个集群可以对外提供一致的服务。
###### Leaders and Replicas
    The concept of a leader is similar to that of master when thinking of traditional Solr replication. The leader is
    responsible for making sure the replicas are up to date with the same information stored in the leader.
    However, with SolrCloud, you don't simply have one master and one or more "slaves", instead you likely have
    distributed your search and index traffic to multiple machines. If you have bootstrapped Solr with numShards=2,
    for example, your indexes are split across both shards. In this case, both shards are considered leaders. If you
    start more Solr nodes after the initial two, these will be automatically assigned as replicas for the leaders.
    Replicas are assigned to shards in the order they are started the first time they join the cluster. 
    This is done in a round-robin manner, unless the new node is manually assigned to a shard with the shardId 
    parameter during startup. This parameter is used as a system property, as in -DshardId=1, the value of which 
    is the ID number of the shard the new node should be attached to.
    On subsequent restarts, each node joins the same shard that it was assigned to the first time the node was
    started (whether that assignment happened manually or automatically). A node that was previously a replica,
    however, may become the leader if the previously assigned leader is not available.
举例说明:    
我们启动一个solr实例A，注册到一个单机zk上，指定参数numShards=2，然后启动solr实例B并注册到这个zk上，此时A和B就满足了
两个分片的配置，此时A和B都是leader。然后我们启动实例C注册到此zk上，此时C会成为A的replica，因为我们指定的两个shard已经
被A和B占据，我们也没有明确指定C属于哪一个shard。相同的，当我们继续启动一个实例D时，他会变成B的一个replica。重启的时候，
如果我们先于A启动C或者先于B启动D，那么先启动的实例会变成leader。
#### SolrCloud的数据分片和索引
    Before SolrCloud, Solr supported Distributed Search, which allowed one query to be executed across multiple
    shards, so the query was executed against the entire Solr index and no documents would be missed from the
    search results. So splitting the core across shards is not exclusively a SolrCloud concept. There were, however,
    several problems with the distributed approach that necessitated improvement with SolrCloud:
        1.Splitting of the core into shards was somewhat manual.
        2.There was no support for distributed indexing, which meant that you needed to explicitly send documents
        to a specific shard; Solr couldn't figure out on its own what shards to send documents to.
        3.There was no load balancing or failover, so if you got a high number of queries, you needed to figure out
        where to send them and if one shard died it was just gone.
    SolrCloud fixes all those problems. There is support for distributing both the index process and the queries
    automatically, and ZooKeeper provides failover and load balancing. Additionally, every shard can also have
    multiple replicas for additional robustness.
    In SolrCloud there are no masters or slaves. Instead, there are leaders and replicas. Leaders are automatically
    elected, initially on a first-come-first-served basis, and then based on the Zookeeper process.
    If a leader goes down, one of its replicas is automatically elected as the new leader. As each node is started, 
    it's assigned to the shard with the fewest replicas. When there's a tie, it's assigned to the shard with the 
    lowest shard ID.
    When a document is sent to a machine for indexing, the system first determines if the machine is a replica or a
    leader.
        1.If the machine is a replica, the document is forwarded to the leader for processing.
        2.If the machine is a leader, SolrCloud determines which shard the document should go to, forwards the 
        document the leader for that shard, indexes the document for this shard, and forwards the index notation 
        to itself and any replicas
在SolrCloud时代到来之前，Solr提供了主从模式，此时也有shard的概念，一个逻辑上的index会被分解到多个shard上，一个搜索请求
会横跨这几个shard，然后每个shard上的查询结果会被合并起来。但此时存在如下几个问题：   
1.分片过程是人工的    
2.没有分布式索引的支持，这意味着Solr不知道将一条新增的document数据放到哪一个分片    
3.没有统一配置管理，没有负载均衡和灾备机制    
SolrCloud解决了这些问题，首先提供了集群内部对索引和查询的路由，然后使用zk提供了灾备和负载均衡。在SolrCloud中使用了leader和
replica的概念代替了zk的master和slave概念。leader首先由最先启动的节点担当，之后由zk的选主机制来维护。当添加一个新的document
到index上时，集群首先判断接收数据的节点的角色，分发到replica上的数据会被路由到leader处理，然后leader负责同步数据到replica。

##### Document Routing - 索引数据路由
    Solr offers the ability to specify the router implementation used by a collection by specifying the router.name 
    parameter when creating your collection. The value can be either implicit, which uses an internal default hash, 
    or compositeId, which allows defining the specific shard to assign documents to. When using the 'implicit' router, 
    the shards parameter is required. When using the 'compositeId' router, the numShards parameter is required. 
    If you use the "compositeId" router, you can send documents with a prefix in the document ID which will be used to 
    calculate the hash Solr uses to determine the shard a document is sent to for indexing. 
    The prefix can be anything you'd like it to be , but it must be consistent so Solr behaves consistently. 
    For example, if you wanted to co-locate documents for a customer, you could use the customer name or ID as 
    the prefix. If your customer is "IBM", for example, with a document with the ID "12345", you would insert the 
    prefix into the document id field:"IBM!12345". The exclamation mark ('!') is critical here,as it distinguishes 
    the prefix used to determine which shard to direct the document to.
    Then at query time, you include the prefix(es) into your query with the _route_ parameter (i.e., q=solr&_rout
    e_=IBM!) to direct queries to specific shards. In some situations, this may improve query performance because
    it overcomes network latency when querying all the shards.
    The compositeId router supports prefixes containing up to 2 levels of routing. For example: a prefix routing
    first by region, then by customer: "USA!IBM!12345".
    Another use case could be if the customer "IBM" has a lot of documents and you want to spread it across
    multiple shards. The syntax for such a use case would be : "shard_key/num!document_id" where the /num is the
    number of bits from the shard key to use in the composite hash.
    So "IBM/3!12345" will take 3 bits from the shard key and 29 bits from the unique doc id, spreading the tenant
    over 1/8th of the shards in the collection. Likewise if the num value was 2 it would spread the documents across
    1/4th the number of shards. At query time, you include the prefix(es) along with the number of bits into
    your query with the _route_ parameter (i.e., q=solr&_route_=IBM/3!) to direct queries to specific shards.
    If you do not want to influence how documents are stored, you don't need to specify a prefix in your document ID.
    If you created the collection and defined the "implicit" router at the time of creation, you can additionally 
    define arouter.field parameter to use a field from each document to identify a shard where the document belongs. 
    If the field specified is missing in the document, however, the document will be rejected. You could also use 
    the _route_ parameter to name a specific shard.
在创建collection的时候Solr提供了通过router.name参数指定router规则的功能。取值有implicit和compositeId两种。使用implicit的时候
需要指定shards参数，使用compositeId的时候，需要指定numShards参数。    
我们以后者为例，我们通过给document的ID加前缀，solr会计算这个前缀的哈希值，根据哈希值和numShards确定在索引时将索引数据存储到
哪一个shard上。例如，如果你希望根据用户名称将索引分片，那你可以将用户名作为ID的前缀，比如："zhanglaibao!12345678"，
注意这个!是必须的分隔符，查询的时候你需要用_route_参数将前缀带入查询条件，例如"\_route\_=zhanglaibao!"，注意要包含这个!分隔符。
compositeId前缀可以支持两级，比如："zhubajie!zhanglaibao!12345678"。
##### Shard Splitting
    When you create a collection in SolrCloud, you decide on the initial number shards to be used. But it can be
    difficult to know in advance the number of shards that you need.
    The ability to split shards is in the Collections API. It currently allows splitting a shard into two pieces. The
    existing shard is left as-is, so the split action effectively makes two copies of the data as new shards. You can
    delete the old shard at a later time when you're ready.
在创建collection的时候需要指定numShards参数，但是我们很难预知这个参数的合理值。所以solr提供了Collection API支持shard分裂，
当前支持一个shard分裂成两份，原有shard不受影响，当新的shard数据准备好之后原有shard可以被删掉。
##### Ignoring Commits from Client Applications in SolrCloud - 忽略客户端显式提交
在大多数情况下，当在SolrCloud模式下运行时，索引客户端应用程序不应发送显式提交要求。相反应该使用openSearcher = false和
自动软提交配置使数据的更新在后续的搜索请求中可见。要强制客户端应用程序不应发送显式提交的策略，你需要修改每一个客户端的代码，
这无疑是不合理的，所以solr提供了如下几种配置来实现这一约束：

    <!-- solrconfig.xml -->
    
    <!-- the processor will return 200 to the client but will ignore the commit/optimize request -->
    <updateRequestProcessorChain name="ignore-commit-from-client" default="true">
        <processor class="solr.IgnoreCommitOptimizeUpdateProcessorFactory">
            <int name="statusCode">200</int>
        </processor>
        <processor class="solr.LogUpdateProcessorFactory" />
        <processor class="solr.DistributedUpdateProcessorFactory" />
        <processor class="solr.RunUpdateProcessorFactory" />
    </updateRequestProcessorChain>   
     
    <!-- the processor will raise an exception with a 403 code with a customized error message --> 
    <updateRequestProcessorChain name="ignore-commit-from-client" default="true">
        <processor class="solr.IgnoreCommitOptimizeUpdateProcessorFactory">
            <int name="statusCode">403</int>
            <str name="responseMessage">Thou shall not issue a commit!</str>
        </processor>
        <processor class="solr.LogUpdateProcessorFactory" />
        <processor class="solr.DistributedUpdateProcessorFactory" />
        <processor class="solr.RunUpdateProcessorFactory" />
    </updateRequestProcessorChain>
    
    <!-- ignore optimize and let commits pass thru --> 
    <updateRequestProcessorChain name="ignore-optimize-only-from-client-403">
        <processor class="solr.IgnoreCommitOptimizeUpdateProcessorFactory">
             <str name="responseMessage">Thou shall not issue an optimize, but commits are OK!</str>
             <bool name="ignoreOptimizeOnly">true</bool>
        </processor>
        <processor class="solr.RunUpdateProcessorFactory" />
    </updateRequestProcessorChain>

#### Distributed Requests - 分布式请求处理
##### Limiting Which Shards are Queried - 限定搜索的分片
搜索所有分片
    
    http://localhost:8983/solr/gettingstarted/select?q=*:*
搜索某一分片    

    http://localhost:8983/solr/gettingstarted/select?q=*:*&shards=localhost:7574/solr
搜索某些分片    

    http://localhost:8983/solr/gettingstarted/select?q=*:*&shards=localhost:7574/solr,localhost:8983/solr
在某些分片中自动负载均衡搜索请求   
 
    http://localhost:8983/solr/gettingstarted/select?q=*:*&shards=localhost:7574/solr|localhost:7500/solr
当然，更简单的方式是使用zookeeper中shard的ID作为shards参数的值。

##### Configuring the ShardHandlerFactory - ShardHandlerFactory配置
Solr的默认配置是吞吐量优先，当然它提供了大量可配置参数使用户可以细粒度配置solr的行为特性，配置方法如下：

    <!-- solrconfig.xml -->
    <requestHandler name="standard" class="solr.SearchHandler" default="true">
        <!-- other params go here -->
        <shardHandler>
            <shardHandlerFactory class="HttpShardHandlerFactory">
            <int name="socketTimeOut">1000</int>
            <int name="connTimeOut">5000</int>
        </shardHandler>
    </requestHandler>
##### Configuring statsCache implementation
Solr需要document和term的统计数据用来计算相关性。Solr提供了以下四种document统计数据的实现：    
1.LocalStatsCache:默认值。 仅使用本地term和document统计数据来计算相关性，当term在各个shard中分布比较均匀时，计算结果很准确。    
2.ExactStatsCache:使用全局docunment频率。    
3.ExactSharedStatsCache: 与2类似，但是对相同的查询条件会重用全局统计信息。    
4.LRUStatsCache:使用LRU算法统计。
    
    <!-- solrconfig.xml -->
    <statsCache class="org.apache.solr.search.stats.ExactStatsCache"/>
    
##### Avoiding Distributed Deadlock - 避免死锁
    Each shard serves top-level query requests and then makes sub-requests to all of the other shards. Care should
    be taken to ensure that the max number of threads serving HTTP requests is greater than the possible number
    of requests from both top-level clients and other shards. If this is not the case, the configuration may result in a
    distributed deadlock.
    For example, a deadlock might occur in the case of two shards, each with just a single thread to service HTTP
    requests. Both threads could receive a top-level request concurrently, and make sub-requests to each other.
    Because there are no more remaining threads to service requests, the incoming requests will be blocked until the
    other pending requests are finished, but they will not finish since they are waiting for the sub-requests. By
    ensuring that Solr is configured to handle a sufficient number of threads, you can avoid deadlock situations like
    this.
SolrCloud在处理一个搜索请求时，每个分片会提供一个“顶级”查询服务，然后向其他所有分片发起子查询请求。所以在配置时要提供足够的
线程去处理并发的查询请求以避免死锁，例如：    
我们有两个分片A和B，他们都被配置成只有一个提供HTTP服务的线程，他们在并发处理请求时会出现这样的场景：A在处理一个“顶级”查询，
然后向B发起一个子查询，如果此时B也同样向A发出子查询，由于两个分片都没有额外的线程去处理对方发起的查询，那么这两个节点的请求
就会一直等待，形成死锁。

##### Prefer Local Shards - 本地分片优先
    Solr allows you to pass an optional boolean parameter named preferLocalShards to indicate that a
    distributed query should prefer local replicas of a shard when available. In other words, if a query includes prefe
    rLocalShards=true, then the query controller will look for local replicas to service the query instead of
    selecting replicas at random from across the cluster. This is useful when a query requests many fields or large
    fields to be returned per document because it avoids moving large amounts of data over the network when it is
    available locally. In addition, this feature can be useful for minimizing the impact of a problematic replica with
    degraded performance, as it reduces the likelihood that the degraded replica will be hit by other healthy replicas.
    Lastly, it follows that the value of this feature diminishes as the number of shards in a collection increases
    because the query controller will have to direct the query to non-local replicas for most of the shards. In other
    words, this feature is mostly useful for optimizing queries directed towards collections with a small number of
    shards and many replicas. Also, this option should only be used if you are load balancing requests across all
    nodes that host replicas for the collection you are querying, as Solr's CloudSolrClient will do. If not
    load-balancing, this feature can introduce a hotspot in the cluster since queries won't be evenly distributed
    across the cluster.
Solr提供了boolean类型的preferLocalShards参数来指定本地分片优先策略。如果我们在一次请求中传递preferLocalShards=true，那么
集群会选择检索本地分片而不是在整个集群里选择其他节点上的分片。当我们的查询结果数据量很大的时候，这一特性可以减少数据在集群
实例间传输的网络开销。另外，这一特性减少了数据损坏的分片的命中率，从而提高集群的性能。换言之，当集群中分片数较少但是副本数
较多时，这一特性会显著减少网络开销，提升性能。并且，只有集群间有负载均衡机制的时候才能使用这一参数，CloudSolrClient就是这
样做的，否则在集群中很容易形成热点，因为请求不会被均匀的分发到集群的其他节点