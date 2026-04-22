# 日程表 App

## 项目简介
日程表 App 是一个基于 Android 原生 Java 开发的手机端日程管理应用，面向个人日常计划、学习安排、工作待办和重要事项提醒等场景。项目以本地离线可用为核心，当前已经具备登录、资料完善、月历首页、今日日程、新建与编辑日程、重复日程、排序、本地提醒和提醒设置等能力。

## 当前已完成功能
- 登录与资料完善流程
- 首页月历视图与今日日程列表
- 日程创建、编辑、删除、完成标记
- 手动拖拽排序与按时间自动排序
- 重复日程配置、范围编辑与删除
- 本地提醒与提醒设置第一页
- 中文界面与移动端交互优化

## 技术栈
- 开发语言：Java
- 平台：Android
- 架构：MVVM + Repository
- 本地数据库：Room + SQLite
- 通知与提醒：WorkManager + 本地通知
- UI：Material Design 3

## 目录结构
```text
app/src/main/java/com/example/calendar/
├── common      # 常量、公共工具
├── data        # 数据库、DAO、Repository
├── domain      # 领域模型与用例
├── reminder    # 本地提醒与调度
├── threading   # 并发调度相关封装
└── ui          # 各页面与 ViewModel
```

## 后续计划
- 数据备份与恢复（JSON / Excel）
- 日视图与周视图
- 循环提醒与更完整的提醒策略
- 主题切换与设置中心完善
- 更多单元测试与稳定性验证

## 编码约束
- 项目源代码、资源文件、文档统一使用 UTF-8 编码
- 新增用户可见文案优先写入 `strings.xml` 等资源文件
- 提交前建议执行 `testDebugUnitTest` 与 `assembleDebug`

