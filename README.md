<div align="center">

<img src="docs/assets/c001apk-revived-rounded.png" width="128" alt="c001apk-Revived 图标">

<h1>c001apk-Revived</h1>

<p>基于 Jetpack Compose 的酷安第三方客户端</p>

<p>
  <a href="https://github.com/frisk1127/c001apk-Revived/stargazers"><img src="https://img.shields.io/github/stars/frisk1127/c001apk-Revived?style=flat&amp;logo=github&amp;label=Stars&amp;color=e3b341" alt="GitHub Stars"></a>
  <a href="https://github.com/frisk1127/c001apk-Revived/releases"><img src="https://img.shields.io/github/downloads/frisk1127/c001apk-Revived/total?label=Downloads&amp;color=2da44e" alt="累计下载量"></a>
  <a href="https://github.com/frisk1127/c001apk-Revived/releases"><img src="https://img.shields.io/github/v/release/frisk1127/c001apk-Revived?include_prereleases&amp;sort=date&amp;label=Release&amp;color=4f8cc9" alt="最新版本（含预发布版）"></a>
  <img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&amp;logoColor=white" alt="Android 7.0+">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-AGPL--3.0-blue" alt="AGPL-3.0"></a>
</p>

<p>
  <a href="https://github.com/frisk1127/c001apk-Revived/releases">下载应用</a> ·
  <a href="CHANGELOG.md">更新日志</a> ·
  <a href="https://github.com/frisk1127/c001apk-Revived/issues">问题反馈</a>
</p>

</div>

在 [c001apk-compose](https://github.com/bggRGjQaUbCoE/c001apk-compose) 的基础上继续维护，提供 MD3 与 miuix 两种界面，支持多账号切换、动态浏览、图文发布、评论回复、消息与私信，以及实况图片、HDR 显示、产品参数对比和收藏备份。

## 界面预览

miuix 深色模式，点击图片可查看原图。

<table>
  <tr>
    <th align="center">首页</th>
    <th align="center">我的</th>
    <th align="center">设置</th>
  </tr>
  <tr>
    <td align="center" width="33%"><a href="docs/assets/screenshots/miuix-home.jpg"><img src="docs/assets/screenshots/miuix-home.jpg" width="260" alt="miuix 首页：板块标签、推荐内容和动态列表"></a></td>
    <td align="center" width="33%"><a href="docs/assets/screenshots/miuix-mine.jpg"><img src="docs/assets/screenshots/miuix-mine.jpg" width="260" alt="miuix 我的页面：账号概览、常用功能和互动通知"></a></td>
    <td align="center" width="33%"><a href="docs/assets/screenshots/miuix-settings.jpg"><img src="docs/assets/screenshots/miuix-settings.jpg" width="260" alt="miuix 设置页面：界面风格、主题设置和浏览选项"></a></td>
  </tr>
</table>

## 功能

- **双界面** — MD3／miuix 可即时切换，无需重启。miuix 覆盖首页、动态、评论、私信、登录和设置等页面，并提供按压缩放、返回动效及动态渐变关于页。
- **背景模糊** — miuix 可选择全局模糊，或仅模糊顶栏、底栏和标签栏；后者不模糊弹窗。
- **账号管理** — 支持添加、切换、移除账号；提供账号密码与网页登录。登录成功后自动尝试获取数字联盟 ID（szlmid）并提示结果，后续可手动刷新或填写。
- **实况与 HDR 图片** — 支持实况图片播放、声音开关、重播与保存，以及 Ultra HDR 图片显示。
- **图片浏览** — 全屏预览、手势缩放、滑动切换与长按保存，支持保存原始 GIF。
- **产品参数** — 原生参数页面，支持同机型不同配置和跨机型对比，最多同时比较 10 组配置。
- **原生众测** — 在应用内查看众测详情、活动规则和报名须知，并进入报名页面。
- **首页与横屏** — 自定义首页板块显示与顺序，支持一键回顶；横屏首页全宽显示，打开详情后切换为左右双栏。
- **发布与回复** — 支持选择 `@` 用户、`#` 话题和系统相册图片，评论中展示回复对象头像与回复关系；支持 30 个牛牛表情的显示及发送。
- **我的与收藏** — 汇集账号概览、常用功能、互动通知和消息；本地收藏支持图片预览、导出和导入 JSON 备份，导入时自动合并去重，保留收藏时间。
- **搜索与跳转** — 站内搜索与搜索历史，支持通过酷安帖子链接和搜索链接直接打开对应页面。
- **主题与触感** — Material You 动态取色、自定义主题、深色与纯黑模式、字体和内容缩放；可切换 Revived／原版图标，均支持系统取色；触觉反馈可开关、调节强度并预览。

> 背景模糊需要 Android 13 及以上；Ultra HDR 需要 Android 14 及以上和兼容设备；实况播放需要图片附带视频数据。

## 下载

前往 [Releases](https://github.com/frisk1127/c001apk-Revived/releases) 下载，支持 **Android 7.0 及以上**。日常使用建议选择稳定版，想提前体验新功能可以选择预发布版。各版本的具体变化见 [更新日志](CHANGELOG.md)。

从旧版 c001apk-compose 升级时，同签名且版本号不低于已安装版本的安装包可直接覆盖安装，保留原有数据。收藏备份入口位于 **我的 → 本地收藏 → 右上角菜单**。

## 常用入口

| 功能 | 入口 |
| --- | --- |
| 切换 MD3／miuix | 设置 → 主题 → 界面风格 |
| 模糊开关和范围 | miuix：设置 → 主题设置 → 界面效果 |
| 添加或切换账号 | 我的 → 顶部账号卡片右侧的账号管理按钮 |
| 手动刷新数字联盟 ID | 设置 → 数字联盟 ID → 自动获取并保存 |
| 收藏备份 | 我的 → 本地收藏 → 右上角菜单 |
| 问题反馈 | 设置 → 关于 → 反馈 |

## 开发

使用 Kotlin 和 Jetpack Compose，`main` 为主线。版本号由 Git 提交记录生成，本地构建请保留完整 Git 历史。

推送到 `main`、`dev`、`miuix` 或向这些分支提交 PR 时，CI 会自动构建 Debug APK，并运行应用与媒体模块的单元测试。构建产物和测试报告可在对应的 [Actions](https://github.com/frisk1127/c001apk-Revived/actions/workflows/ci.yml) 运行页面下载；纯文档改动跳过自动检查，也可手动触发。

<details>
<summary>本地构建</summary>

需要 JDK 21、Android SDK 37 和 Build Tools 36.0.0。在项目根目录执行：

```powershell
.\gradlew assembleDebug
.\gradlew assembleRelease
.\gradlew :app:testDebugUnitTest :media-support:testDebugUnitTest
```

APK 输出至 `app/build/outputs/apk/` 对应目录。签名信息通过不纳入 Git 的 `local.properties` 配置。

Release 默认开启 R8，排查混淆问题时可使用 `assembleRelease -PdisableR8=true` 构建。

</details>

## 致谢与许可

感谢 [c001apk-compose](https://github.com/bggRGjQaUbCoE/c001apk-compose)、[Miuix](https://github.com/compose-miuix-ui/miuix) 及各项目贡献者。第三方组件及许可信息可在应用的开源许可证页面和 [第三方声明](THIRD_PARTY_NOTICES.md) 中查看。

感谢 [LINUX DO](https://linux.do/) 社区为开源项目提供交流与展示的空间。

本项目采用 [AGPL-3.0](LICENSE) 许可证。
