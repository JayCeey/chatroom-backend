# chatroom聊天室后端
## 技术栈
- 数据库：mysql
- 消息队列： rookiemq
- oss存储：minio
- 配置管理、服务中心：nacos
- 消息传输：websocket
- 服务降级：hystrix
- 网关：springcloud gateway
- 缓存：redis
- 用户索引查询：ELK
- 数据库同步工具：canal

## 项目架构
- api: 提供微服务RPC调用接口，这里用feign
- common: 公共工具模块，如消息队列、utils工具类、minio存储、cache缓存、通用安全认证等
  - security: 安全认证模块
    - 对所有请求（除了某些特定域）进行JWT的用户鉴权，并且附加上请求的用户信息
    - 双令牌单点登录
- friend: 朋友服务
- gateway: 网关
- group: 群组服务
- message: 消息服务（重点）
  - 提供websocket服务
  - 使用rocketmq实现异步发送通知以及通知其他用户功能
- notice: 通知服务
  - 服务端流式推送通知（服务端单向推送），不关闭HTTP连接而是持续发送HTTP连接，比websocket轻量
- user: 用户服务

