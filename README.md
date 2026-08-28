# 易耗品更换管家（ConsumableTracker）

家庭易耗品更换记录 App，原生安卓（Kotlin + Jetpack Compose + Room）。数据全部本地存储，无网络权限，不联网、不对外发送任何个人信息。

## 功能
- **清单页**：按地址筛选；物品卡片显示下次到期、剩余天数、进度条、状态（正常 / 即将到期 / 已逾期）。
- **录入**：右下角「+」填写物品名称、地址（可输新地址）、类别、首次更换日期、周期（月 / 年），实时计算下次到期。
- **详情**：点卡片进入，查看更换历史；可「记录本次更换」追加日期，或删除某条历史。
- **提醒**：到期写入**系统日历日程**（不是闹钟 / 系统通知）；可设提前 7 / 15 / 30 / 60 天。
- **统计**：总数 / 即将到期 / 已逾期 + 未来 6 个月到期分布。
- **备件库存**（底部第 4 个 Tab「备件」）：增删改备件，字段为名称 / 存放位置（地址）/ 数量 / 备注；卡片上直接 +/− 调数量，按存放位置筛选。

## 技术栈
- Kotlin + Jetpack Compose + Material3
- Room（本地数据库）+ DataStore（轻量配置）
- 系统日历日程做到期提醒（CalendarContract）
- Gradle 8.9（已通过 `gradle-wrapper.properties` 锁定，clone 后 `./gradlew` 自动下载对应版本）

## 构建与运行
1. 用 Android Studio（Hedgehog 或更新）打开 `ConsumableTracker` 目录；或直接使用仓库自带的 Gradle Wrapper：`bash gradlew assembleDebug`。
2. 首次同步会下载依赖，需联网。
3. 连接安卓设备并开启「USB 调试」（部分机型如 OriginOS 还需在开发者选项允许「USB 安装」），或启动模拟器。
4. 安装 `app/build/outputs/apk/debug/app-debug.apk`。
5. 首次启动会请求日历读写权限（READ_CALENDAR / WRITE_CALENDAR），允许后与日历账户联动，到期提醒才会写入系统日历。

## 数据存储
- Room 数据库，三张表：`items`（含 `calendarEventId` 字段）、`replacements`、`spare_parts`（独立备件表）。
- 数据库当前版本为 v5，采用 `fallbackToDestructiveMigration()`：升级时会整体重建（测试阶段会清空旧数据，生产数据请先备份）。

## 提醒机制（写入系统日历）
不依赖闹钟 / 系统通知，而是把每条物品的「下次更换」写入**系统日历日程**：
- 每次新增 / 删除 / 记录更换都会重新同步：事件从「下次更换 − 提前天数」当天 09: 00 开始，到「下次更换日」09: 00 结束，事件开始时由日历自动提醒（即提前 N 天）。
- 事件归属标记采用日历事件**描述**中的 `#id=<物品id>`（例如「易耗品更换提醒 · 周期 3月 · #id=12」），用于重装 / 换机后定位并覆盖旧事件。
  > 早期版本曾用 `_sync_id` 字段标记，但该字段仅系统同步适配器可写，在部分机型（如 OriginOS）上会导致启动崩溃，现已改为描述内嵌标记。
- 需要日历读写权限；若本机没有任何可写日历账户（如未登录日历账号），则跳过写入、不弹提醒，App 其余功能不受影响。

## 目录结构
```
app/src/main/java/com/wangxq/consumable/
  data/        数据库与实体（AppDatabase、ItemEntity、SparePart 等）
  ui/          界面与 ViewModel（AppUi.kt 为主界面，MainViewModel 为逻辑中枢）
  reminder/    日历日程写入（CalendarReminder）
  util/        日期与状态计算（DateUtils）、CSV 导出（CsvExporter）
```

## 隐私
- App 不申请网络权限，所有数据保留在设备本地，不上传任何服务器。
- debug 包默认 `allowBackup=true`，可通过系统备份迁移到新设备。
