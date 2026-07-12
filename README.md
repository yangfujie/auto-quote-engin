启动说明
1. 数据库准备
   创建MySQL数据库：CREATE DATABASE aqe;

修改 application.yml 中的数据库连接信息。

启动时Flyway会自动执行迁移脚本。

2. 启动后端
   导入后端项目为Maven工程，运行 AutoQuoteEngineApplication.java。

默认端口8080。

3. 启动前端
   进入 frontend 目录，执行 npm install 安装依赖。

执行 npm run dev，访问 http://localhost:5173（Vite默认端口）。

4. 测试流程
   先创建一个策略定义（可在前端编辑器画好流程图，目前需在数据库中预先插入节点数据，或通过前端保存）。

创建实例，设置触发条件和优先级。

模拟发送行情：可通过POST请求 http://localhost:8080/api/market/simulate（需添加一个Controller端点），或直接在 MarketDataNode 中静态调用 updateMarket。为了方便，我们可以在MonitorController加一个模拟端点：