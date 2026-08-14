# 媒体兼容 fixture 目录

这是 E09 的 fixture 入口。目录契约位于 `app/src/test/java/com/futureape/kanleme/data/media/MediaFixtureCatalogTest.kt`，它先固定覆盖范围、来源和隐私声明，再允许逐个补入实际媒体样本。

## 覆盖范围

| fixture id | 格式/故障 | 当前状态 | 来源要求 |
| --- | --- | --- | --- |
| `jpeg_standard` | 标准 JPEG | synthetic contract | 生成字节，不含 EXIF 个人信息 |
| `jpeg_long_image` | 长图 JPEG | licensed sample required | 自制或明确许可样本 |
| `gif_animation` | GIF | synthetic contract | 生成最小动画字节 |
| `raw_dng` | RAW/DNG | licensed sample required | 明确许可，保留来源与许可证 |
| `heif_photo` | HEIC/HEIF | licensed sample required | 明确许可，保留来源与许可证 |
| `corrupt_image` | 损坏文件 | synthetic contract | 生成截断/错误头 |
| `zero_byte` | 零字节 | synthetic contract | 生成空文件 |
| `motion_photo_tail` | JPEG 尾部实况视频 | synthetic contract | 生成 `ftyp`/`moov` 契约字节 |
| `motion_vendor_variant` | 不同厂商实况格式 | licensed sample required | 获许可 OEM 样本 |
| `long_tail_video` | 超长尾部视频 | licensed sample required | 合成或获许可的大文件样本 |
| `unreadable_uri` | 不可读 URI | synthetic contract | 测试 resolver/权限失败路径 |

## 隐私与提交边界

- 合成样本可以进入单元测试；真实媒体必须去除 EXIF、位置、设备序列号和可识别人脸等个人信息
- 每个外部样本必须记录来源、许可证、格式、文件大小和校验摘要；没有记录不得进入仓库或 CI
- 真实 OEM/HEIC/RAW 文件默认放在仓库外的受控 fixture 目录，CI 未配置时保持 `not-run`
- 不以 fixture 元数据测试通过冒充真实解码、缩略图或实况播放通过

## 当前验证边界

现有单元测试已经覆盖合成实况尾部的合法 `ftyp` + `moov` 与损坏 movie box。真实 OEM、HEIC/HEIF、RAW、超大尾部视频和设备 MediaStore 授权路径仍需要获许可样本与连接设备后执行；这部分在目录中保持明确的 `licensed sample required` 状态。
