# 狂躁雪人（Frenzied Snow Golem）实现规格

> 状态：**已实现并通过服务端验证**（召唤成功、生命 20、不融化、4 个环境机制）。
> 待玩家实测：索敌/攻击、刷怪蛋外观、自然生成速率。
> 设计要点：敌对性**直接绑定天气**，所以后期"打倒 BOSS → 停止下雪 → 雪人恢复温和"不需要改代码。

## 1. 身份

| 项 | 值 |
|---|---|
| 实体 id | `frostrealm:frenzied_snow_golem` |
| 名称 | 狂躁雪人 / Frenzied Snow Golem |
| 基类 | `net.minecraft.world.entity.animal.SnowGolem` |
| 生命 | **20**（10 颗心） |
| 尺寸 | 0.7 × 1.9（与原版雪人一致） |
| 分类 | `MobCategory.MONSTER` |
| 刷怪蛋 | `frostrealm:frenzied_snow_golem_spawn_egg`，主色白 `0xF2F5FA`、斑点红 `0xD8323A` |

贴图：复制原版 `textures/entity/snow_golem.png`，只把**眼睛像素改成红色**（脚本
`build/tools/gen_frenzied_snow_golem.py`：在头部区域找最深色像素簇 → 换成 `#D8323A`）。

渲染：`class FrenziedSnowGolemRenderer extends SnowGolemRenderer`，只重写
`getTextureLocation()` 返回 `frostrealm:textures/entity/frenzied_snow_golem.png`（原版渲染器把贴图写死了）。

## 2. 敌对性：绑定天气（关键设计）

```java
public boolean isFrenzied() {
    return level().isRaining();      // 霜境的 isRaining() 由 LevelWeatherMixin 恒为 true
}
```

- 霜境因为 `LevelWeatherMixin`（`getRainLevel` 恒等 1.0）永远在下雪 → `isRaining()` = true → 敌对
- 后期 BOSS 被打倒后，把霜境天气改成不下雪（去掉/关闭 mixin 分支，或加一个配置/状态位让
  `FrostWeather.isBlizzard()` 返回 false）→ `isRaining()` 变 false → 雪人自动恢复温和，**无需改雪人代码**
- 建议在 `FrostWeather` 里加一个静态开关 `blizzardSuppressed`（由 BOSS 击败逻辑置位），
  这样"停止下雪"就是一个布尔值，而不是删 mixin

## 3. 行为

- **目标**：`NearestAttackableTargetGoal<Player>`，条件 `isFrenzied()`；温和时清除目标
  （`setTarget(null)`，并在 `customServerAiStep` 里检查 `!isFrenzied()`）
- **攻击**：沿用雪人 `RangedAttackGoal`（间隔 20 tick、射程 10），但弹射物换成自定义
  `frostrealm:frost_snowball`（`FrostSnowball extends Snowball`）：
  - `onHitEntity`：`target.hurt(damageSources().thrown(this, owner), 1.0F)` + 沿弹道方向击退
    （`knockback(0.5, dx, dz)`），然后 `discard()`
  - 原版 `Snowball` 对玩家是 0 伤害，所以必须自定义
- 生命 20 用 `createAttributes()`：`MAX_HEALTH → 20`，其余同雪人
- 掉落：可选，建议 `snowball` 0–2（不必须）

## 4. 生成

不进群系 spawn list，改用机制类自定义生成（更好控制"上方 4 格空气"和"不受亮度影响"）：

```java
public final class FrenziedSnowGolemMechanic implements EnvironmentMechanic {
    // 每 20 tick，对每个在霜境的玩家做若干次尝试（对等原版僵尸的每区块尝试率）
    // 1) 随机点：玩家 ±20 格水平、±10 格垂直
    // 2) 要求 pos..pos+3 四格都是空气（"上方 4 格内均为空气"）
    // 3) 要求 pos.below() 是实心方块（可站立）
    // 4) 概率与僵尸一致：约 1/10 每次尝试（原版 monster 每区块 1 次尝试、成功率 ~10%）
    // 5) 生成 FrenziedSnowGolem 并 setPersistenceRequired()（避免自然消失导致玩法体验差，
    //    可选；如果要像普通怪物一样能消失就不要这行)
}
```

- **不看光照**：上面没有任何 `getLightLevel` 判断
- 只在 `FrostRealmApi.isFrostRealm(level)` 生效

## 5. 需要新建/修改的文件

| 文件 | 内容 |
|---|---|
| `world/entity/FrenziedSnowGolem.java` | 实体（生命、`isFrenzied`、目标、发射自定义雪球） |
| `world/entity/FrostSnowball.java` | 1 伤害 + 击退的雪球弹射物 |
| `world/gen/FrenziedSnowGolemMechanic.java` | 自定义生成 |
| `FrostMobs.java` | `ENTITY_TYPES` 注册（雪人 + 雪球）+ 刷怪蛋 + `EntityAttributeCreationEvent` |
| `client/FrostEntityRenderers.java` | `FrenziedSnowGolemRenderer` 注册（`EntityRenderersEvent.RegisterRenderers`） |
| `FrostRealm.java` | 构造函数里加 `FrostMobs.bootstrap()` 与 `registerMechanic(new FrenziedSnowGolemMechanic())` |
| `lang/en_us.json` + `zh_cn.json` | `entity.frostrealm.frenzied_snow_golem`、`item.frostrealm.frenzied_snow_golem_spawn_egg` |
| `textures/entity/frenzied_snow_golem.png` | 红眼雪人贴图（脚本生成） |

## 6. 需要先核对的 1.21.1 API（写代码前查本地源码 jar）

1. `EntityType.Builder.of(...).sized(...).build(...)` 的参数：1.21 起 `build` 需要一个
   `ResourceKey<EntityType<?>>`，确认签名
2. `SpawnEggItem` 构造参数（1.20.5+ 改成 `Supplier<EntityType<? extends Mob>>` 还是直接给 type？）
3. `SnowGolemRenderer#getTextureLocation` 是否可覆写、返回类型
4. `Snowball#onHitEntity` 签名（`(Entity, Entity)`? `HitResult`?）
5. `EntityAttributeCreationEvent#put` 用法（NeoForge 21.1）

源码位置：`build/moddev/artifacts/neoforge-21.1.262-sources.jar`（用 `unzip`/PowerShell 的
`System.IO.Compression` 读），或直接写代码后让编译器告诉你。

## 7. 验收

- [ ] 霜境里站着等，能看到白色雪人在雪地里出现（不受光照影响，洞穴里也能刷）
- [ ] 玩家靠近会被雪球打，**1 点伤害**并**被击退**
- [ ] 生命 20（雪球砸 20 下才死，或用剑测）
- [ ] 刷怪蛋：白底红斑点，放置即生成
- [ ] 停止下雪后（临时把 `FrostWeather.isBlizzard` 改成返回 false 测一次）雪人不再攻击玩家
