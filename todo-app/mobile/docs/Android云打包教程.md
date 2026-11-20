# Android 云打包速查（移动端目录）

> 一句话：在 HBuilderX 里登录 DCloud，导入 `todo-app/mobile`，重新获取 AppID，然后“发行 → 原生App-云打包”。
>
> 本文是移动端目录里的**现场速查版**；完整讲解、每一步看到什么、常见错误，见
> **[../../docs/tutorials/16-Android云打包.md](../../docs/tutorials/16-Android云打包.md)**。

---

## 本章你会

- 用最短路径把一个 APK 打出来
- 知道哪几个地方最容易卡住
- 知道出问题该翻哪一节

---

## 一、操作前检查

| 项目 | 要求 |
|---|---|
| HBuilderX | 5.34+ 正式版 |
| DCloud | 已登录 |
| 工程 | 导入的是 `todo-app/mobile` |
| 项目类型 | 圆形 U，`manifest.json` 含 `"uni-app-x": {}` |
| 后端 | `http://localhost:8080/api/tasks` 能返回 JSON |
| 手机 | 和电脑同一 Wi-Fi |

---

## 二、十步打出 APK

### 1. 导入工程

```
文件 → 导入 → 从本地目录导入 → todo-app/mobile
```

### 2. 登录

HBuilderX 右上角登录 DCloud 账号。

### 3. 重新获取 AppID

```
双击 manifest.json → 基础配置 → 重新获取 AppID
```

> 工程里的 `__UNI__7F3A2C1` 是占位值，**不能直接云打包**。

### 4. 检查配置

| 配置 | 值 |
|---|---|
| 应用名称 | 待办清单 |
| 版本名称 / 版本号 | 1.0.0 / 100 |
| Android 包名 | `com.example.todoapp` |
| 图标 | `static/logo.png` |
| 权限 | INTERNET、ACCESS_NETWORK_STATE |

### 5. 手机浏览器测试后端

```
http://电脑局域网IP:8080/api/tasks
```

能返回 JSON 才继续。

### 6. 打开云打包

```
发行 → 原生App-云打包
```

### 7. 选择配置

| 选项 | 选择 |
|---|---|
| 平台 | 只勾 Android |
| 包名 | `com.example.todoapp` |
| 证书 | 云端证书 |
| 格式 | APK |
| 渠道 | 无 |
| 广告 / SDK | 都不勾 |
| 自定义基座 | 不勾 |
| iOS | 不勾 |

### 8. 提交并等待

控制台出现：

```
提交云端打包 → 排队 → 编译 → 打包完成
```

免费通道可能要等几分钟到十几分钟。

### 9. 下载

产物通常位于：

```
todo-app/mobile/unpackage/release/
```

或：

```
todo-app/mobile/unpackage/release/android/
```

以控制台显示路径为准。

### 10. 安装并设置后端

安装 APK 后，打开 App：

```
右上角 ⚙ → 填 http://电脑局域网IP:8080/api → 保存并测试
```

---

## 三、最容易卡住的三个点

| 现象 | 原因 | 处理 |
|---|---|---|
| AppID 不存在 / 未绑定 | 用了占位 AppID | 重新获取 AppID |
| App 网络异常 | 真机填了 `localhost` | 改成电脑局域网 IP |
| 手机打不开后端 | 防火墙 / 不同 Wi-Fi | 放行 8080，连同一 Wi-Fi |

---

## 四、完整文档

- 技术结构、页面、请求层：[15 移动端](../../docs/tutorials/15-移动端.md)
- 完整云打包步骤、截图位、错误排查：[16 Android 云打包](../../docs/tutorials/16-Android云打包.md)
- 工程说明：[../README.md](../README.md)
