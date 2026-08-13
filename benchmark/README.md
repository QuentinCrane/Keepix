# 看了么性能实验室

这是一个只包含测试代码的 Android benchmark module，目标包仍然是 `com.futureape.kanleme`。它不读取或提交用户照片/视频；fixture 只描述媒体元数据。

## 构建

```powershell
./gradlew.bat :benchmark:assemble
```

## 执行

连接固定的 release-like、profileable 测试设备后执行：

```powershell
./gradlew.bat :benchmark:connectedCheck
```

Macrobenchmark 结果由 AndroidX 输出到 benchmark report。每次记录都要同时保存设备型号、Android/API、编译模式、电量、温度、迭代次数和 fixture scale；没有设备运行时不得把模板中的 `not-run` 改成估计值。
