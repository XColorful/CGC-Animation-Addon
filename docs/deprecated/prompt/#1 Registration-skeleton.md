> #1 提示词

# 

```
对照CGC-Compat-MachineMax的dev.xcolorful.cgccompat.machinemax.core.config.CgccMMConfig，把CGC-Animation-Addon的dev.xcolorful.cgcanimation.client.config一个字段的写入和保存给补全
- 需要额外加一个save()供CgcAnimationClient调用
```

# 

I'll look at both config classes to understand the pattern.

Now I have the full picture. The addon's `CgcAnimationClient` already calls `CgcAnimationConfig.load(configFile)` and `CgcAnimationConfig.save(configFile)`, but the config class is a skeleton with only the default constant. I'll fill it in mirroring `CgccMMConfig`.

Let me verify the file analyzes cleanly via IDEA.

补全了 [CgcAnimationConfig.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/config/CgcAnimationConfig.java)，对照 `CgccMMConfig` 的写法：

- 新增字段 `public static boolean enableAnimator`（默认 `DEFAULT_ENABLE_ANIMATOR`）
- `load(Path)` — 流式解析，文件不存在/字段缺失走默认值，异常记日志；末尾调用 `save()` 回写，保证缺省键也会出现在文件里
- 新增公开 `save(Path)` — 按固定顺序写 JSON，供 `CgcAnimationClient.saveConfig()` 调用
- `_read(JsonReader)` — 按 `CgcAnimationConfigTag.ENABLE_ANIMATOR` 分发，未知键 `skipValue()`

没有改动 `CgcAnimationClient`（它本来就调了 `load`/`save`，之前 `save` 不存在编译不过，现在补齐）。

验证：IDEA 分析 `CgcAnimationConfig` 与 `CgcAnimationClient` 均无 error；未跑 gradle（依赖来自 CurseForge，未验证实际构建）。

两点需要你确认：
- 字段注释我只写了「是否启用动画」——addon 里目前没有任何消费方，语义我不敢替你定，你补一下准确说明。
- `load()` 回写沿用了 compat 的行为（每次启动规范化配置），如果这个配置允许玩家手改且不希望被回写覆盖，说一声，我把回写挪到 `save()` 里。

未提交，等你 review。
