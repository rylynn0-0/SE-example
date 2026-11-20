# 待办清单 · uni-app x 移动端

这是 `todo-app` 的跨平台客户端，使用 **uni-app x** 编写，目标是：

- Android：云打包 APK
- iOS：云打包 IPA（需要 Apple 证书）
- H5：静态站点
- 微信小程序：附加目标

当前先完成 **Android 云打包**。

## 快速开始

1. 用 HBuilderX 5.34+ 导入本目录 `todo-app/mobile`
2. 登录 DCloud 账号
3. 双击 `manifest.json`，点击“重新获取 AppID”
4. 启动后端，确认手机能访问 `http://电脑IP:8080/api/tasks`
5. 发行 → 原生App-云打包 → Android + 云端证书 + APK

完整步骤、常见错误见：

**[docs/Android云打包教程.md](docs/Android云打包教程.md)**

技术结构和调试说明见：

- [15 移动端](../docs/tutorials/15-移动端.md)
- [16 Android 云打包](../docs/tutorials/16-Android云打包.md)

## 目录

```
mobile/
├── App.uvue                 应用入口、全局样式
├── main.uts                 createSSRApp
├── pages.json               页面路由
├── manifest.json            应用名称、AppID、版本、包名、图标
├── AndroidManifest.xml      Android 网络权限、明文 HTTP
├── index.html               H5 入口模板
├── pages/
│   ├── index/index.uvue     首页：统计、搜索、筛选、列表、设置
│   └── edit/edit.uvue       新增/编辑
├── api/
│   ├── config.uts           各平台默认后端地址
│   ├── request.uts          uni.request Promise 封装
│   ├── task.uts             任务接口
│   └── models.uts           Task / TaskPage / TaskStats
├── static/
│   ├── logo.png             1024 图标源图
│   └── icons/               Android 各密度图标
└── docs/
    └── Android云打包教程.md
```

## 后端地址

| 运行环境 | 默认地址 |
|---|---|
| Android 模拟器 | `http://10.0.2.2:8080/api` |
| Android 真机 | 在 App 的“服务器设置”里填 `http://电脑局域网IP:8080/api` |
| H5 | `/api`，由 Nginx 反代 |
| iOS 模拟器 | `http://127.0.0.1:8080/api` |

真机不能用 `localhost`，因为手机上的 `localhost` 指向手机自己。

## 后端新增接口

编辑页需要按 ID 读取任务，后端新增：

```
GET /api/tasks/{id}
```

原分页、新增、修改、删除、统计接口保持不变。

## 实现状态

- [x] uni-app x 工程骨架
- [x] 首页：统计、搜索、筛选、加载更多、服务器设置
- [x] 新增/编辑页
- [x] `uni.request` 封装与统一错误处理
- [x] Android 图标、包名、网络权限
- [x] H5 编译产物
- [ ] Android 云打包 APK（需要在 HBuilderX 登录 DCloud 后操作）
- [ ] iOS IPA
- [ ] 微信小程序
