# 启动 Trace 记录

D03 只先增加观测点，不凭静态猜测移动初始化。`StartupTrace` 覆盖 Application、Activity、ViewModel 首次 scope/bootstrap、照片/视频首页查询和 MediaStore 同步；异步区段使用 async trace，可在 dispatcher 切换后正确闭合。

## 设备记录

连接固定设备后先确认：

```powershell
adb devices
```

再使用 Android Studio System Trace/Perfetto 或 Macrobenchmark 报告记录冷启动、首页首帧、照片首图、授权后首次同步，并保留设备型号、Android/API、构建类型、温度、电量和迭代次数。不要把 Debug 或无设备的静态检查写成 TTID/TTFD。

## 本阶段结论

- `KanlemeApplication.onCreate()` 的缓存裁剪已经在独立 daemon 线程执行，本块只标记调度成本，未改变时序
- `MainActivity` 的密度计算和 Compose 内容设置仍保持原顺序，避免首开首页预览竞态
- ViewModel 的首个照片预览/队列查询与授权后的 MediaStore 同步已有独立 trace 区段
- 当前设备列表为空，因此 D03 还没有足够 trace 证据决定延迟初始化或缩小首屏查询窗口
