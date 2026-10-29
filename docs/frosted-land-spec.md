# 结霜之地（Frosted Land）实现规格

> 状态：**规格，待实现**。建议新开一轮/新会话执行。
> 复用点都标了现有文件，照着改能省一半工作。

## 1. 群系 `frostrealm:frosted_land`

- 地形：**平坦**，参考草原（plains）。做法与现有 10 个 `frost_*` 群系一致：
  复制原版 `plains` 的 biome JSON，保留 `carvers`（洞穴）、把 `raw_generation` 换成现有
  `frostrealm:frost_floor`，矿物换成霜矿 + 煤矿（和 `frost_snowy_plains.json` 一样），
  **删掉所有树木 feature**（`trees_*`、`patch_*` 里的树），保留雪层与草。
- 地表覆盖雪：沿用现有 frost 群系的 snow 处理（`frost_snowy_plains.json` 里已有）。
- 生物群系源：把 `frostrealm:frosted_land` 加进
  `data/frostrealm/dimension/permafrost.json` 的 `snowy_biomes` 列表，并在
  `FrostBiomeMap` 里把 `minecraft:plains` 映射到它？——**不要**，那会把所有平原改成结霜之地。
  更合适：用 `FrostBiomeSource` 里加一个按坐标哈希的「平原小概率变成结霜之地」分支
  （例如 `hash % 8 == 0` 的 plains 位置 → `frosted_land`），这样它像一片片独立区域。

## 2. 巨木（fallen giant log）

- 方块：`frostrealm:giant_log`（+ `giant_wood`？只需 log 即可），属性同橡木原木，
  贴图：原版 `oak_log` 调整为**更深的棕色**（脚本重映射色相/明度）。
- 合成：
  - `giant_planks`（4 个，形状同原木→木板）
  - `stick`（两个 giant_planks 竖排，走原版 `stick` 配方 或 自定义）
  - 工具：`giant_wooden_pickaxe/sword/axe/shovel`，直接用原版木制工具改色（脚本把
    `wooden_*` 贴图整体偏深棕），属性复制 `Tier.WOOD` / `ToolMaterial`。
- 生成：`FallenLogFeature`（新 feature），在结霜之地地表随机放 3–7 格长的横躺原木，
  内部按需求用**冰块填充**（把原木空腔/内部替换成 `minecraft:ice` 或 `glacier_ice`），
  概率参考现有的稀疏 decoration（`rarity_filter` + `count`）。

## 3. 易碎冰块 `frostrealm:thin_ice`

- 贴图：原版 `ice` 改亮、提高透明度、加裂纹（脚本绘制）。
- 行为：
  - `strength` 参考玻璃（0.3F，无爆炸抗性）
  - **不可采集**：战利品表为空（`pools: []`），但可以被破坏（挖掘/爆炸）
  - 实体站上去计时 **3 秒（60 tick）** → 变成 `minecraft:water`
  - 变成水后 **5 分钟（6000 tick）** → 变回 `thin_ice`
  - 实现建议：
    - 用 `EntityInsideBlockEvent`/`Block#stepOn` 不好做计时，建议方块自带
      `entityInside` + `BlockEntity` 或 `scheduleTick`：
      - `entityInside` 记录「有实体」，用 `level.scheduleTick(pos, this, 60)` 安排破坏
      - 恢复用 `level.scheduleTick(pos, WATER, 6000)`（水无法 scheduleTick → 用
        `FrostThinIceRestorer implements EnvironmentMechanic` 维护一个
        `Map<BlockPos, Long>` 到期队列，在 `onLevelTick` 里把到期的水换回薄冰）
  - 状态机的到期表建议存进现有的 `RegionLedger` 同款 `SavedData`，避免重进世界丢计时。

## 4. 湖泊

- `FrozenLakeFeature`：在结霜之地找低洼处，挖 4–7 格深、半径 6–10 的盆地，
  填水；**中心 3×3 ~ 5×5 不结冰**，其余水面铺 `frostrealm:thin_ice`
- 湖里内容：`cod`（原版鳕鱼实体）+ `seagrass`/`kelp`（原版水下植物），
  用原版 `seagrass` patch feature 或直接在 feature 里放方块即可
- 湖的生成位置要记进 `RegionLedger`（kind = `"lake"`），方便以后地图用

## 5. 食人兔 `frostrealm:carnivorous_rabbit`

- 继承 `net.minecraft.world.entity.animal.Rabbit`
- 贴图：原版 `rabbit` 贴图改红眼/偏灰白（脚本）
- 属性：`MAX_HEALTH = 10`、`ATTACK_DAMAGE = 1.5`、沿用兔子移动速度
- AI：
  - `NearestAttackableTargetGoal<LivingEntity>`（排除 Rabbit 同类 + 排除自己）
  - `MeleeAttackGoal(this, 1.3, true)`（主动攻击）
  - 不再吃胡萝卜：不实现 `isFood`，且不要把胡萝卜加入 `rabbit_food` 标签
- 生成：只加入 `frosted_land` 的 `spawners.creature`（weight 小，如 2），
  并复用 `FrostSpawnRules` 的白名单（要把它加进 `ALLOWED`，否则会被拒）
- 掉落：战利品表 `entity/carnivorous_rabbit.json`：`rabbit`（兔肉）概率 ~50%，
  可参考 `rabbit` 原版表

## 6. 需要新建/修改的文件清单

| 文件 | 内容 |
|---|---|
| `data/frostrealm/worldgen/biome/frosted_land.json` | 平原副本、无树、含雪、霜矿 |
| `FrostBiomeSource` | plains → frosted_land 的坐标哈希分支 |
| `dimension/permafrost.json` | `snowy_biomes` 加入新群系 |
| `world/block/GiantLogBlock`（或直接用 `RotatedPillarBlock`） | 巨木方块 |
| `world/block/ThinIceBlock` | 易碎冰 |
| `world/gen/FallenLogFeature` + configured/placed JSON | 倒塌巨木 |
| `world/gen/FrozenLakeFeature` + configured/placed JSON | 湖 |
| `world/gen/ThinIceRestorer`（EnvironmentMechanic） | 水→薄冰 恢复计时 |
| `world/entity/CarnivorousRabbit` | 食人兔 |
| `FrostMobs` | 注册实体 + 刷怪蛋 + 属性事件 |
| `client/FrostEntityRenderers` | `RabbitRenderer` 复用（换贴图） |
| 贴图脚本 `gen_frosted_land.py` | 巨木、木板、4 把工具、薄冰、兔子 |
| 配方 JSON（木板/木棍/4 工具 + 巨木相关） | datapack |
| `FrostSpawnRules.ALLOWED` | 加入 `carnivorous_rabbit` |

## 7. 验收

- [ ] `/locate biome frostrealm:frosted_land` 能找到
- [ ] 地表平坦、无树、有雪
- [ ] 能看到横躺的深棕色巨木，内部是冰
- [ ] 湖面大部分是薄冰，中心是水；水里能钓到鳕鱼、有水草
- [ ] 站上薄冰 3 秒后变成水，5 分钟后变回薄冰
- [ ] 薄冰挖掉不掉落
- [ ] 食人兔主动攻击玩家和别的生物，10 血、1.5 伤害、不认胡萝卜
- [ ] 巨木能合成木板、木棍、木镐/剑/斧/铲
