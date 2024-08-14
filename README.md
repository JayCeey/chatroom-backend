# chatroom聊天室后端
## 技术栈
- 数据库：mysql
- 消息队列： rookiemq
- oss存储：minio
- 分布式事务：seata
- 配置管理、服务中心：nacos
- 消息传输：websocket
- 服务降级：hystrix
- 网关：springcloud gateway
- 缓存：redis
- 消息记录查询：ELK

## 项目架构
- api: 提供微服务调用接口
- common: 公共工具，如消息队列、utils工具类、minio存储等
- friend: 朋友服务
- gateway: 网关
- group: 群组服务
- message: 消息服务
- notice: 通知服务
- user: 用户服务
