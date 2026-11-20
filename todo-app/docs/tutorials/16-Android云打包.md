# 16 · Android 云打包：从工程到 APK

> 一句话：**不用装 Android Studio，用 HBuilderX 登录 DCloud，把 `mobile/` 提交到云端编译，下载 APK 安装到手机。**
>
> 本篇是纯操作文档。技术原理、目录结构、跨平台地址策略看 [15 移动端](15-移动端.md)。

---

## 本章你会

- 把 `todo-app/mobile` 导入 HBuilderX
- 生成并绑定自己的 DCloud AppID
- 检查包名、版本、图标、权限
- 用手机浏览器确认后端可访问
- 提交 Android 云打包
- 下载 APK、安装到手机
- 在 App 里设置后端地址
- 排查 AppID、证书、网络、安装失败

---

## 一、云打包是什么

普通 Android 打包需要：

```
JDK + Android SDK + Gradle + 签名证书
```

云打包把这些放到 DCloud 的服务器上：

```
你的电脑（HBuilderX）
    │ 上传工程
    ▼
DCloud 云端编译
    │ 生成 APK
    ▼
下载到本地 → 传到手机安装
```

你只需要：

1. 装 HBuilderX；
2. 登录 DCloud；
3. 点“发行 → 云打包”。

不需要安装 Android Studio，也不需要配置 Gradle。

---

## 二、操作前检查表

| 项目 | 要求 | 在哪检查 |
|---|---|---|
| HBuilderX | 5.34 或更高正式版 | 启动页/关于 |
| DCloud 账号 | 已登录 | HBuilderX 右上角 |
| 工程目录 | `todo-app/mobile` | 文件管理器 |
| 后端 | 已启动，8080 可访问 | 浏览器 |
| 手机 | Android 5.0+ | 手机设置 |
| 网络 | 手机和电脑同一 Wi-Fi | 手机 Wi-Fi 设置 |
| AppID | 已绑定你的账号 | `manifest.json` |

> 如果 HBuilderX 解压很慢，先看本文最后的“解压慢怎么办”。

---

## 三、导入工程

### 3.1 打开 HBuilderX

下载地址：

<https://www.dcloud.io/hbuilderx.html>

下载 Windows 正式版 `.zip`，解压后运行：

```
HBuilderX.exe
```

建议解压到短英文路径，例如：

```
C:\Dev\HBuilderX
```

不要放在 OneDrive 同步目录或很深的桌面路径里。

### 3.2 导入目录

菜单：

```
文件 → 导入 → 从本地目录导入
```

选择：

```
todo-app/mobile
```

注意：

- 选 `mobile` 目录本身；
- 不要选 `todo-app`；
- 不要选仓库根目录；
- 不要只复制 `pages` 文件夹。

### 3.3 确认项目类型

导入成功后，左侧项目树的图标应该是**圆形 U**。

如果图标是普通文件夹：

1. 右键项目；
2. 点“重新识别项目类型”；
3. 再确认 `manifest.json` 里有：

```json
"uni-app-x": {}
```

这个标记工程里已经写好了。

---

## 四、登录 DCloud 并获取 AppID

### 4.1 登录

点击 HBuilderX 右上角头像/“登录”：

1. 输入 DCloud 账号；
2. 没有账号就注册：<https://dev.dcloud.net.cn/>；
3. 登录成功后右上角显示头像/昵称。

云打包和 AppID 都依赖登录状态。

### 4.2 为什么必须重新获取 AppID

工程里的 AppID 是占位值：

```json
"appid": "__UNI__7F3A2C1"
```

它没有绑定你的账号。直接拿它云打包，会提示：

```
AppID 不存在
AppID 未绑定
```

### 4.3 获取步骤

1. 双击 `manifest.json`；
2. 打开“基础配置”；
3. 找到“AppID”；
4. 点击“重新获取 AppID”/“获取 AppID”；
5. 登录状态下，HBuilderX 会自动申请；
6. 成功后 `appid` 变成类似：

```json
"appid": "__UNI__A1B2C3D"
```

**这个值以后不要随便改。** 它关联你的应用身份和云打包记录。

> 注意区分两个 AppID：
>
> - 这里的 AppID 是 **DCloud AppID**，以 `__UNI__` 开头；
> - 微信小程序也有 AppID，是 `wx...` 开头，两者不是一回事。
>
> 同样，Android 包名 `com.example.todoapp` 也不是 AppID，它只是 Android 系统里的应用标识。

---

## 五、检查配置

双击 `manifest.json`，按下面逐项确认。

### 5.1 基础配置

| 配置项 | 应该是什么 |
|---|---|
| 应用名称 | `待办清单` |
| 应用描述 | `待办清单 · uni-app x 跨平台客户端` |
| 版本名称 | `1.0.0` |
| 版本号 | `100` |
| 应用图标 | `static/logo.png` |

版本号规则：

```
versionName = 1.0.0    给人看
versionCode = 100      给系统比较大小
```

以后发版：

```
1.0.1 / 101
1.1.0 / 110
```

`versionCode` 必须递增，否则手机可能不提示更新。

### 5.2 Android 包名

工程当前配置：

```json
"packagename": "com.example.todoapp"
```

课程演示可以保留。想改就改成：

```
com.你的名字拼音.todo
```

规则：

- 至少两段，用点分隔；
- 每段小写字母开头；
- 只能包含小写字母、数字、下划线；
- 不能有中文、空格、横杠；
- 不要以数字开头。

> HBuilderX 不同版本对 manifest 字段的组织略有差异。
> 如果“安卓App配置”里看不到包名，不用手改源码，直接在云打包弹窗里填写即可；
> 图标同理，在可视化界面重新选择 `static/logo.png`，HBuilderX 会写入它认识的格式。

### 5.3 Android 权限

`manifest.json` 已配置：

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

没有 `INTERNET`，App 会一直网络异常。

### 5.4 明文 HTTP

工程根目录 `AndroidManifest.xml` 已配置：

```xml
<application android:usesCleartextTraffic="true" />
```

因为开发阶段后端是 `http://电脑IP:8080`。

正式发布时建议改成 HTTPS，并把这行设为 `false`。

### 5.5 图标

工程有两套：

```
static/logo.png          1024×1024，源图
static/icons/            48/72/96/144/192
```

如果 HBuilderX 提示图标不合格：

1. 双击 `manifest.json`；
2. 找到“应用图标”；
3. 重新选择 `static/logo.png`；
4. 让 HBuilderX 自动生成各尺寸。

---

## 六、先确认手机能访问后端

**不要等 APK 装完才发现网络不通。** 先用手机浏览器验证。

### 6.1 启动后端

如果已经装好 Docker，最简单的方式是在 `todo-app` 目录执行：

```bash
cd todo-app
docker compose up -d --build
docker compose ps
```

`docker compose ps` 里四个服务都应该是 `running` / `healthy`。然后确认：

```
http://localhost:8080/api/tasks
```

能返回 JSON。

如果不用 Docker，需要本机安装 JDK 17、Maven、MySQL、Redis，再按
[todo-app/README.md 第五节](../../README.md#五快速开始) 的本地开发方式启动。

> 手机端只关心后端 8080 端口，不关心你是 Docker 还是本地 Maven 启动的。

### 6.2 查电脑 IP

PowerShell：

```powershell
ipconfig
```

找到当前 Wi-Fi 的 IPv4，例如：

```
192.168.1.10
```

### 6.3 手机浏览器测试

手机和电脑连同一个 Wi-Fi，打开：

```
http://192.168.1.10:8080/api/tasks
```

能返回 JSON 才继续。

### 6.4 打不开怎么办

| 检查 | 操作 |
|---|---|
| 防火墙 | 放行 8080 入站 |
| 同一网段 | 手机和电脑都连同一个路由器 |
| AP 隔离 | 学校/公司 Wi-Fi 可能禁止互访，换手机热点 |
| 后端监听 | Spring Boot 默认监听 0.0.0.0 |
| IP 写错 | 用 `ipconfig` 重新确认 |

### 6.5 App 里填什么

| 运行环境 | 地址 |
|---|---|
| Android 模拟器 | `http://10.0.2.2:8080/api` |
| Android 真机 | `http://电脑局域网IP:8080/api` |

真机示例：

```
http://192.168.1.10:8080/api
```

注意末尾要带 `/api`。

---

## 七、提交云打包

> HBuilderX 不同小版本的按钮文案可能略有差异。认准关键词：
> **发行、原生App、云打包、Android、云端证书、APK**。
> 如果找不到“原生App-云打包”，就找“App-Android/iOS-云打包”。

### 7.1 打开打包界面

菜单：

```
发行 → 原生App-云打包
```

部分 HBuilderX 版本显示为：

```
发行 → App-Android/iOS-云打包
```

### 7.2 平台

只勾选：

```
Android
```

不要勾 iOS。iOS 需要 Apple 证书，后面再做。

### 7.3 包名

填写：

```
com.example.todoapp
```

或你改好的包名。

### 7.4 证书类型

新手选：

```
云端证书
```

云端证书由 DCloud 自动生成：

- 不用自己生成 keystore；
- 不用记证书密码；
- 首次可能等待几十秒生成；
- 以后更新要继续用同一张证书。

如果你有自己的证书，才选“自选证书”，并填：

```
证书库文件
证书库密码
证书别名
证书密码
```

### 7.5 打包格式

选：

```
APK
```

不要选 AAB。AAB 主要用于 Google Play，国内手机直接安装不方便。

### 7.6 渠道

选：

```
无
```

### 7.7 广告 / SDK / 自定义基座

全部不勾。

特别注意：

```
不要勾“打包自定义基座”
```

自定义基座是调试用的，不是最终安装包。

### 7.8 提交

勾选同意协议，点击：

```
打包
```

或：

```
提交
```

---

## 八、等待与下载

提交后看 HBuilderX 底部控制台：

```
正在提交云端打包...
云端打包排队中...
编译中...
打包完成
```

免费通道可能排队几分钟到十几分钟，属于正常现象。

成功后：

1. 控制台出现下载链接；
2. 可能弹出“打包成功”窗口；
3. 产物一般在：

```
todo-app/mobile/unpackage/release/
```

或：

```
todo-app/mobile/unpackage/release/android/
```

文件名类似：

```
__UNI__XXXXXXX__20261120.apk
```

以控制台显示的实际路径为准。

> 如果只看到 `.wgt`，那不是安装包。`.wgt` 是热更新资源包，不能直接安装。

---

## 九、安装到手机

### 9.1 传输

任选一种：

- 数据线复制到手机；
- 微信“文件传输助手”；
- QQ 发送到手机；
- 局域网文件传输工具。

### 9.2 安装

在手机文件管理里点 APK：

1. 允许“安装未知应用”；
2. 点击“安装”；
3. 如提示风险，选择“继续安装”。

### 9.3 安装失败

| 提示 | 原因 | 处理 |
|---|---|---|
| 解析包错误 | APK 下载不完整 | 重新下载 |
| 应用未安装 | 旧版签名不同 | 先卸载旧版 |
| 版本号低于已安装版本 | versionCode 没增加 | 改大后重新打包 |
| 不支持的 Android 版本 | minSdk 太高 | 当前是 21，一般没问题 |

---

## 十、第一次打开 App

### 10.1 设置后端

1. 打开 App；
2. 点右上角“⚙”；
3. 填：

```
http://192.168.1.10:8080/api
```

4. 点“保存并测试”；
5. 成功会提示：

```
连接成功，共 8 条任务
```

6. 返回首页。

### 10.2 验收清单

- [ ] 首页显示总数、未完成、已完成
- [ ] Redis 状态显示“回源”或“命中”
- [ ] 搜索“报告”能筛出对应任务
- [ ] 筛选“未完成/已完成”正常
- [ ] 新增任务成功，列表和统计更新
- [ ] 编辑任务能回填并保存
- [ ] 点击圆钮能切换完成状态
- [ ] 删除有二次确认
- [ ] 加载更多能翻页
- [ ] 关掉 App 再打开，服务器地址仍保留

### 10.3 每一步的“完成标志”

新手最怕“点完了不知道对不对”。对照这张表：

| 步骤 | 完成标志 |
|---|---|
| 导入工程 | 左侧出现 `mobile`，项目图标是圆形 U |
| 重新获取 AppID | `manifest.json` 里不再是 `__UNI__7F3A2C1` |
| 手机浏览器测试 | 页面显示 `{"code":200,...}` 这样的 JSON |
| 提交云打包 | 控制台出现“打包完成”或下载链接 |
| 下载 APK | 文件后缀是 `.apk`，不是 `.wgt` |
| 安装 APK | 手机桌面出现“待办清单”图标 |
| 设置后端 | 提示“连接成功，共 8 条任务” |
| 首页刷新 | 三个统计数字和任务列表都出现 |

如果某一步没有出现对应的标志，先停下来查那一节，不要继续往下点。

---

## 十一、常见错误

### 11.1 AppID 不存在 / 未绑定

原因：

- 还在用占位 `__UNI__7F3A2C1`；
- 没有登录 DCloud；
- 获取 AppID 时网络失败。

处理：

```
manifest.json → 基础配置 → 重新获取 AppID
```

### 11.2 云打包按钮灰色

可能原因：

- 没有登录；
- 工程没被识别成 uni-app x；
- 当前打开的不是项目文件；
- HBuilderX 版本过低。

处理：

1. 登录；
2. 右键项目 → 重新识别项目类型；
3. 打开 `manifest.json` 再试；
4. 升级 HBuilderX。

### 11.3 包名不合法

检查：

- 是否至少两段；
- 是否以数字开头；
- 是否包含中文、横杠、空格。

推荐：

```
com.yourname.todo
```

### 11.4 证书错误

新手直接用“云端证书”。

如果自选证书，确认：

- keystore 文件没选错；
- 证书库密码和别名密码不是一回事；
- 别名拼写正确。

### 11.5 云打包排队很久

免费通道有排队，正常。

可以：

- 等；
- 避开高峰；
- 去 DCloud 开发者中心看打包记录。

### 11.6 App 打开白屏

1. 看 HBuilderX 控制台有没有编译错误；
2. 确认打包时没勾“自定义基座”；
3. 确认 HBuilderX 版本和工程匹配；
4. 重新打包，不要手改 `unpackage` 里的文件。

### 11.7 App 网络异常

按顺序查：

1. 手机浏览器能否打开 `http://电脑IP:8080/api/tasks`；
2. App 地址是否带 `/api`；
3. 是否填了 `localhost`；
4. 防火墙是否放行 8080；
5. 手机和电脑是否同一 Wi-Fi；
6. `usesCleartextTraffic` 是否为 true。

### 11.8 修改代码后重新打包，版本没变

检查：

```json
"versionName": "1.0.1",
"versionCode": "101"
```

`versionCode` 必须比上一次大。

### 11.9 Kotlin 编译错误（进阶）

云打包日志里出现 `.kt` 文件报错时，问题通常在 UTS 写法，不在 Java 后端。常见四类：

| 报错 | 原因 | 修复 |
|---|---|---|
| `Unresolved reference 'loadAll'` / `'goBack'` / `'today'` | UTS 编译到 Android 后是 Kotlin 局部函数，**不能像 JS 一样先用后定义** | 把被调用的函数移到调用它的函数前面 |
| `Unresolved reference 'detail'` | 事件参数写成了 `any`，Android 端无法推断 `event.detail` | 日期选择器用 `UniPickerChangeEvent` 类型 |
| `options['id']` 报 `String.get` 接收者不匹配 | `onLoad` 参数写成了 `any` | 改成 `OnLoadOptions`，或用 `getString('id')` |
| `Cannot infer type ... RequestOptions` | `uni.request` 的泛型 `T` 无法从空 `data` 推断 | 给 `success` 参数标 `RequestSuccess<UTSJSONObject>`，并调用 `uni.request<UTSJSONObject>(options)` |

示例：

```ts
// 错误：先调用、后定义，Android 端会报 Unresolved reference
onLoad(() => { loadAll() })
async function loadAll() { ... }
```

```ts
// 正确：先定义，后调用
async function loadAll() { ... }
onLoad(() => { loadAll() })
```

```ts
// 日期选择器：不要写 event: any
function onDateChange(event: UniPickerChangeEvent) {
  dueDate.value = event.detail.value as string
}
```

```ts
// 请求泛型：让 T 有明确来源
success: (res: RequestSuccess<UTSJSONObject>) => { ... }
uni.request<UTSJSONObject>(options)
```

---

## 十二、解压 HBuilderX 很慢怎么办

HBuilderX 压缩包约 205 MB，但解压后文件非常多，慢通常不是压缩包大小，而是小文件太多。

建议：

1. 用 7-Zip，不要用资源管理器“全部解压缩”；
2. 解压到短英文路径：

```
C:\Dev\HBuilderX
```

3. 不要放桌面 / OneDrive；
4. 临时把目标目录加入 Windows Defender 排除项；
5. 不要中途反复取消，否则会缺 DLL；
6. 卡在 `uni-agent/knowledges` 时，可以排除该目录，它不影响云打包。

---

## 十三、发版注意

1. **保存证书**
   - 云端证书可在 DCloud 开发者中心下载；
   - 换证书后不能覆盖安装旧版。

2. **AppID 不要随便换**
   - 换 AppID 等于换应用身份。

3. **versionCode 必须递增**
   - 否则部分手机不提示更新。

4. **正式环境用 HTTPS**
   - 把 `usesCleartextTraffic` 改成 false；
   - 后端配置 HTTPS 证书。

5. **上架应用商店**
   - 需要软著、隐私政策、备案等；
   - 课程演示只要 APK 能装能跑即可。

---

## 十四、最小操作路径

```
1. 下载 HBuilderX 正式版，解压到 C:\Dev\HBuilderX
2. 打开 HBuilderX，登录 DCloud
3. 文件 → 导入 → todo-app/mobile
4. 双击 manifest.json → 重新获取 AppID
5. 确认包名 com.example.todoapp、版本 1.0.0/100、图标
6. 启动后端，手机浏览器访问 http://电脑IP:8080/api/tasks
7. 发行 → 原生App-云打包
8. 勾 Android，证书选云端证书，格式 APK，渠道无
9. 提交，等待完成
10. 下载 APK，传到手机安装
11. App 右上角设置 http://电脑IP:8080/api
```

---

## 十五、本章小结

- 云打包不需要 Android Studio，但必须登录 DCloud。
- 占位 AppID 不能打包，必须“重新获取 AppID”。
- 真机不能用 `localhost`，要用电脑局域网 IP。
- 新手证书选“云端证书”，格式选 APK。
- 打包完成先看控制台路径，再把 APK 传到手机。
- 网络不通先查防火墙、IP、同一 Wi-Fi。

上一篇：

← [15 移动端](15-移动端.md)

返回目录：

← [00 开始这里](00-开始这里.md)
