### Overview - 概述
当我们向Solr发起一个搜索请求，请求首先会被request handler处理。Request handler被设计成可插拔的，可以通过配置来选择，
用来盛放处理请求时的逻辑。Solr提供了大量request handler，有一些用来处理搜索请求，还有一些用来处理比如建立索引这样的请求。
Request handler调用query parser来解析搜索语句中的terms和parameters，不同的query parser支持不同的语法，Solr的默认
query parser是DisMax query parser，同时也提供了早期Lucence用的standard query parser和一个Extended DisMax (eDisMax) 
query parser。Standard query parser得到的查询结果更精确，DisMax query parser的设计初衷是为了给用户提供像Google
这种搜索引擎产品一样的用户体验，所以容错性更好，极少将语法错误展现给用户。Extended DisMax query parser是DisMax 的升级版，
在支持高容错性的基础上增加对standard query parser的支持，提供更高的精确度，还有一些扩展的功能。    
以下是query parser都应该支持的查询参数：
* search strings, terms to search for in the index - 搜索字符串，用于检索索引
* parameters for fine-tuning the query by increasing the importance of particular strings or fields, by
applying Boolean logic among the search terms, or by excluding content from the search results - 微调参数，
比如提高某字段的权重或者排除某个字段
* parameters for controlling the presentation of the query response, such as specifying the order in which
results are to be presented or limiting the response to particular fields of the search application's 
schema. - 控制应答呈现方式的参数，比如排序，返回字段选取等

搜索参数中还可以指定query filter，作为搜索应答的一部分，query filter会检索整个索引并缓存结果。由于Solr为query filter
提供了独立的缓存，索引使用query filter策略可以提升搜索的性能。    
搜索请求中可以指定高亮显示结果中的某些字段，即solr支持多字段高亮，并且提供了很多参数来控制高亮展示的方式。   
搜索结果还可以被配置成带有snippet(document 摘录)来说明高亮的文本。流行的搜索引擎，像Google和Yahoo!都会在搜索结果中
返回3-4行文本来对结果进行简介。
为了帮助对要搜索的内容一无所知的用户，solr提供了两种搜索辅助：分类和聚类。分类是将搜索结果根据某些索引字段分成不同的类目，
在每个类目中，solr展示了本类目下的记录数量。聚类是指在执行搜索，而非建立索引时，将相似的结果分组的操作。聚类的结果通常
没有明确地继承关系，但是聚类仍然很有用，它可以揭示搜索结果数据之间出人意料的共性，并且可以帮助对搜索数据并不耐心的用户获取更多信息。
Solr还提供了一个叫做MoreLikeThis的支持，可以使用户基于先前查询的结果发起新的搜索，MoreLikeThis可以配合分类和聚类使用，
为用户提供搜索帮助。    
下图是第一个使用solr的网站，CNET的截图，用来说明以上概念：
![solr-faceting-clustering.png](https://github.com/ZhangLaibao/machine_gun/blob/master/images/solr-faceting-clustering.png)

Solr组件response writer决定了查询结果的最终呈现方式，solr支持了很多种response writers，
包括XML Response Writer和JSON Response Writer.
下图用来图解一个请求的处理过程：
![solr-request-handle.png](https://github.com/ZhangLaibao/machine_gun/blob/master/images/solr-request-handle.png)

### Query Syntax and Parsing - 查询语法和解析
#### 通用查询参数
在简介中我们提到，solr提供的query parser支持一些相同的查询参数，以下就是这些查询参数的简介：

|Parameter|Description| 
|----|-----| 
|defType|指定处理当前请求的query parser.|
|sort|根据相关度或者其他指定条件排序|
|start|从第几条开始读取结果，默认0|
|rows|返回结果条数，默认10|
|fq|对查询结果使用query filter|
|fl|指定返回结果集中的字段，这些字段都必须配置成stored=true|
|debug|返回调试信息：debug=timing仅返回查询时间信息；debug=results返回每条结果的expain信息；debug=query返回全部调试信息.|
|explainOther|在debug信息之外的Lucence调试信息|
|timeAllowed|最长处理时间，如果时间到了，就返回已查询到的数据.|
|omitHeader|省略返回数据中的header，默认是false|
|wt|指定格式化应答的response writer|
|logParamsList|solr默认记录所有入参，这一参数可以指定日志记录哪些参数|
##### The defType Parameter
    defType=dismax
##### The sort Parameter
这一参数适用于数字类型和字符串类型的数据，排序方向不区分大小写

### Faceting - 
### Highlighting - 高光
### Spell Checking - 拼写检查
### Query Re-ranking -结果重排序
### Transforming Result Documents - 结果转换
### Suggester/MoreLikeThis - 推荐
### Pagination - 分页
### Grouping/Clustering - 分组/聚类
### Spatial Search - 空间搜索
### The Terms Component/Term Vector Component/Stats Component/Query Elevation Component - 组件
### Response Writers
### Near Real Time Searching - 准实时搜索
### Exporting Result Sets & Streaming Expressions - 导出大型数据集
