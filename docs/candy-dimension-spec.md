# 糖果维度（Candy Dimension）设计规格

> 状态：**三期全部实现，构建通过**。第三期：`candy`/`candy_corn` 食物（快速食用 + 口渴 -0.5 格）、
> `candy_corn_block` + 两个分解配方、果冻弹性与 -20% 移速、巨型棒棒糖 feature（3 种形状，1/7 区块）。
>
> 后续修正：糖果/果冻不再是「一个方块 + color 状态」，而是 **32 个独立注册的方块**
> （`red_candy_block`、`light_blue_jelly_block` …），每色一个物品，建筑玩家能在创造栏里直接拿到
> 指定颜色；旧的 `candy_block[color=x]` / `jelly_block[color=x]` 仍保留注册（旧存档兼容），
> 中键选取会重定向到对应颜色的新方块，挖掉也能保持颜色。
> 已明确的三个决策：丘陵地形 / 全部 16 色（糖果与果冻为纯色，双色只用于棒棒糖）/ 竖直地狱门样式 + 粉白动态条纹。

## 1. 入口

- 只能**从霜境**进入，主世界搭的门不生效。
- 方块 `frostrealm:frost_honey`（霜之蜜糖）：橘色糖果方块，四周镶一圈冰；不可食用。
- 竖直**地狱门样式**框架（内部 2×3、外框 4×5，与下界门同构），全部由霜之蜜糖搭成。
- **手持** `frostrealm:candy_syrup`（甘果糖浆）右键门框内部 → 点亮：内部 2×3 变成 `frostrealm:candy_portal`。
- 点亮后框架整体替换为 `frostrealm:candy_rock`（糖果门框方块，不可破坏，同 FrostRockBlock 逻辑：
  门被破坏则回退成霜之蜜糖，可拆）。
- 传送面贴图：**粉白相间条纹 + 动画**（16×N 帧竖向条带流动）。
- 复用现有 `FrostPortalBlock`/`FrostEvents` 的蓄力机制与 `PortalCharge`，但**轴向为竖直**（axis x/z），
  触发方式为右键使用甘果糖浆而不是扔钻石。

## 2. 维度

`frostrealm:candy`

- **地形**：有山有谷但落差不大，类似丘陵 —— 直接复用 overworld `multi_noise` 委托（同霜境做法），
  用自定义 BiomeSource 返回糖果群系。
- **天空**：黑夜、时间定格、**偏粉**（不刺眼）→ dimension_type `fixed_time` = 18000（午夜），
  客户端在糖果维度覆盖 `ComputeSkyColor`/`ComputeFogColor` 为暗粉色。
- **底部**：`Y -3 ~ 0` 随机基岩（参考 FrostFloorFeature 的密度曲线）。
- **糖霜** `frostrealm:sugar_frost`：白色、沙状（受影响于重力，可下落），
  把基岩以上的空隙填到**随机高度 ≤ Y6**，形成起伏的糖霜地基。
- **方块替换**（按主世界地形规则生成后整体替换）：
  - 石头类 → `frostrealm:candy_block`（16 色）
  - 草方块/泥土 → `frostrealm:jelly_block`（16 色，半透明 60%，弹性，走上去 -20% 移速）
  - 水/含水方块 → `frostrealm:syrup`（糖浆流体）
- **棒棒糖**：稀疏生成（像丘陵上的树），3–4 种形状，全部由糖果方块或玉米糖方块构成，尺寸很大。

## 3. 方块清单

| id | 说明 |
|---|---|
| `frost_honey` | 橘色 + 四周冰圈，不可食用，传送门框架材料 |
| `candy_rock` | 门框方块，不可破坏（点亮后替换霜之蜜糖），门破则回退 |
| `candy_portal` | 传送面，竖直、粉白动态条纹、不可破坏 |
| `sugar_frost` | 白色沙状（可下落、可堆） |
| `candy_block` ×16 | 硬质，16 色纯色（同羊毛配色），可分解为 4 个糖果 |
| `jelly_block` ×16 | 史莱姆块特性：弹性、60% 透明、-20% 移速，16 色半透明纯色 |
| `candy_corn_block` | 黄色 + 玉米花纹，可分解为 4 个玉米糖 |
| `syrup`（流体） | 淡粉色，可装桶，不可食用，替代糖果维度的水 |

## 4. 道具清单

| id | 说明 |
|---|---|
| `candy_syrup`（甘果糖浆） | 牛奶状、橘色、铁桶外形；**可食用**：+3 饥饿 / +3 温暖；**不能落地**，只能用于点亮门（右键消耗） |
| `candy` | 可食用：+0.5 饥饿，食用时间 -50%，口渴 -0.5 |
| `candy_corn` | 可食用：+1 饥饿，食用时间 -50%，口渴 -0.5 |
| `syrup_bucket` | 糖浆桶（可放置/回收糖浆） |

## 5. 口渴值（糖果维度专属）

- 独立 10 格，HUD 参考现有 `WarmthOverlay` 的实现（`RegisterGuiLayersEvent` + 同步 attachment）。
- 在糖果维度内显示**口渴**而不是温暖度（温暖度条在该维度隐藏）。
- 衰减：每 **60 秒 -0.5 格**。
- 恢复：药水瓶 +4、苹果 +1、甜菜汤/蘑菇煲 +2。
- 归零：附加**中毒**状态，持续扣血直到死亡（周期性 `hurt` + 中毒粒子/图标）。

## 6. 实现顺序（建议）

1. **第一期：能进去** ✅ 已实现
   `frost_honey` / `candy_syrup` / `candy_portal` / `candy_rock` + 竖直门逻辑 + 糖果维度
   （dimension_type：黑夜定格 + 粉天空）+ 基岩与糖霜地板 + 石头→糖果、草→果冻、水→糖浆的替换。
2. **第二期：生存**
   口渴值 attachment + HUD + 中毒惩罚 + 糖果/玉米糖/甘果糖浆的食用效果。
3. **第三期：内容量**
   16 色糖果/果冻的纯色贴图打磨、玉米糖方块、棒棒糖结构（3–4 种，每个棒棒糖最多混两种颜色）、
   棒棒糖的双色搭配、果冻弹性与减速、分解配方。

## 7. 可复用的现成设施

- 贴图生成：`build/tools/gen_textures.py` / `gen_frost_assets.py`（Python + Pillow，程序化绘制）
- 地形后处理：`FrostFloorFeature`（raw_generation 阶段改地形）
- 群系副本与矿物控制：`gen_frost_assets.py` 里复制原版 biome JSON 的做法
- 模板池注入：`gen_village_pools.py`（若棒棒糖要用结构生成，可直接用 `StructureProcessor` 思路）
- 传送门骨架：`FrostPortalFrame`（框架校验/点火/回退）、`FrostPortalBuilder`（到达侧建造）、
  `FrostEvents#onEntityTick`（蓄力 + 触发器）、`PortalCharge`（蓄力状态）
- 天气/天空：`LevelWeatherMixin`（按维度改天气）、`FrostClientGameEvents`（雾/颜色）
- HUD 与同步：`WarmthOverlay` + `ModAttachments`（`AttachmentType.sync(StreamCodec)`）

---

## 8. 第一期实现记录

### 8.1 新增内容

**方块**（`ModBlocks`）

| id | 类 | 说明 |
|---|---|---|
| `frost_honey` | `Block` | 橘色 + 冰圈，可破坏，门的框架材料 |
| `candy_rock` | `CandyRockBlock` | 不可破坏；周围没有点亮的门时回退为霜蜜糖 |
| `candy_portal` | `CandyPortalBlock` | 竖直 2×3 传送面，`axis=x/z`，粉白动态条纹，无碰撞 |
| `sugar_frost` | `SugarFrostBlock` | `FallingBlock`，重力下落，白色沙状 |
| `candy_block` | `CandyBlock` | 16 色，`color=<dye>` 状态，羊毛式纯色贴图 |
| `jelly_block` | `JellyBlock` | 16 色，半透明（alpha 153），`noOcclusion` + translucent 模型 |
| `syrup` | `LiquidBlock` | 糖浆流体方块，无物品（用桶） |

**物品**：`candy_syrup`（`CandySyrupItem`，点亮门并消耗）、`syrup_bucket`（`BucketItem`）。

**流体**：`ModFluids` — `frostrealm:syrup` / `frostrealm:flowing_syrup` + `frostrealm:syrup` 流体类型
（稠、慢、可游泳，不可造源）。（贴图复用原版水贴图 + 粉色染色，见 8.3。）

**世界生成**：`CandyBiomeSource`、`CandyFloorFeature`、`CandyTerrainFeature`；
数据文件 `dimension/candy.json`、`dimension_type/candy.json`、`worldgen/biome/candy_plains.json`、
`configured_feature` / `placed_feature` 的 `candy_floor` 与 `candy_terrain`。

**门逻辑**：`CandyPortalFrame`（竖直 2×3 / 外框 4×5 校验、点火、门框回退）、
`CandyPortalBuilder`（到达侧建造 / 复用）、`CandySyrupItem`（右键门内点火）。

**客户端**：`ClientLevelSkyMixin`（`ClientLevel#getSkyColor` 覆盖为暗粉）、
`CandyClientGameEvents`（`ComputeFogColor` 暗粉雾）、`FrostFluidClientExtensions`（糖浆渲染与染色）、
`CandyClientColors`（共用色值）。

**资源**：43 个方块模型 + 16+16 张贴图 + 传送面 6 帧动画 + 2 张物品贴图 + 中英文 lang
+ 战利品表（`candy_block`/`jelly_block` 用 `copy_state` 保留颜色）。

### 8.2 实现时的取舍

1. **16 色用「单方块 + `color` 状态」**，而不是 16 个注册方块：注册表更小，世界生成可以直接
   用一个方块按区域选色；物品放置时随机取色，挖下来经 `copy_state` 保留颜色。
2. **`candy_portal` 可破坏且不掉落**（与霜境传送面一致），否则「门被破坏则回退」永远触发不了，
   点亮后的门会无法拆除。`candy_rock` 仍不可破坏，只在门消失后回退成 `frost_honey`（可拆）。
3. **只能在霜境点火**：`CandySyrupItem` 直接判断 `level == frostrealm:permafrost`；
   `CandyPortalBlock` 的传送目标是「霜境 ⇄ 糖果」，在其它维度返回 `null`（不传送）。
   主世界搭出来的门既不亮也不传送。
4. **底部结构**：把 `Y -4 ~ 6` 这一条带整体重写——`-3~0` 按霜境同款密度曲线放随机基岩
   （`-3` 恒为实心，保证糖霜不会下沉），再把基岩以上的空隙逐列填到 `0~6` 的随机高度
   （`sin/cos` 双波，形成起伏糖霜地板）；`Y < -4` 保持原样（不可见，省生成开销）。
5. **地形替换在 `raw_generation`**：原版地表规则（草/沙/水）已在噪声阶段完成，所以替换特征可以
   一次扫完每列；扫描从 `Y 7` 开始，`-4~6` 归地板特征，避免重复写方块。颜色由一片一片的区域决定
   （见 8.6），跨区块稳定。
6. **地形形状完全复用 `minecraft:overworld` 的 multi_noise + noise settings**（与霜境同一做法），
   所有列返回 `frostrealm:candy_plains`，所以丘陵/河谷/海盆与主世界一一对应；主世界的水在糖果维度
   全部变成糖浆（海洋即糖浆海）。
7. **天空**：NeoForge 只有雾色事件、没有天空色事件，而 `fixed_time = 18000` 会让原版把生物群系
   天空色乘以 0（纯黑），因此在客户端 mixin 里直接覆盖 `ClientLevel#getSkyColor` 返回暗玫瑰色；
   雾色走 `ViewportEvent.ComputeFogColor`，两者共用 `CandyClientColors`。
8. **流体注册顺序**：`Fluid` 构造会在流体注册表里分配 intrusive holder，必须在注册事件期间创建；
   NeoForge 按原版顺序触发注册事件，`minecraft:fluid` 早于 `block`/`item`，所以 `ModFluids`
   用 `DeferredHolder` 延迟创建，方块/桶在各自的工厂里 `get()`。

### 8.3 贴图与资源生成

- `build/tools/gen_candy_assets.py`（Pillow 程序化绘制 + 生成 blockstate/model JSON，可重复执行）
- `build/tools/validate_candy_assets.py`（静态校验：blockstate→model→texture 链路、物品模型、
  战利品表、动画帧高，服务端不加载客户端资源，用它兜底）
- `build/tools/candy_test.txt` / `candy_portal_test.txt` / `candy_dupe_test.txt`（RCON 冒烟测试）

### 8.4 验证结果

服务端（`gradlew runServer` + RCON）：

- 基岩/虚空/糖霜地板：`Y -3` 恒为基岩、`Y -4` 为空气、`Y 3` 为糖霜、`Y 4` 为空气 ✅
- 替换：一次 `fill … replace` 统计到 9584 个 `candy_block`、平原区块 468 个 `jelly_block`、
  海洋列 `Y 61` 为 `syrup` ✅；`locate biome frostrealm:candy_plains` 正常 ✅
- 定格午夜：亮度检测器 `power=0` ✅（霜境同款测试读数为 15）
- 门：手工搭 4×5 霜蜜糖框 + 2×3 传送面 → 猪进入后 20 tick 传送到糖果维度，到达侧自动生成
  一座点亮的竖直门（`axis z`），猪落在门内 ✅；出门再进门可原路返回霜境并复用已有的门 ✅；
  敲掉一格门框 → 传送面整体消失、门框回退为霜蜜糖 ✅
- 糖浆流体：可放置、向下流动（`level=8`）、横向铺开 ✅
- 注册表：`candy_syrup` / `syrup_bucket` / `candy_block` 物品均可用 ✅

客户端（`gradlew runClient` + 连接开发服务器，见 `docs/screenshots/`）：

- 资源重载无缺失模型/贴图；`ClientLevelSkyMixin` 成功注入 `ClientLevel` ✅
- 糖果维度：粉色夜空 + 星空、糖果方块山丘、粉白条纹糖果门与飘落花瓣粒子 ✅

### 8.5 已知范围（留给后续期）

- 口渴值、食用效果、中毒惩罚（第二期）。
- 果冻弹性 / -20% 移速（第三期；目前只有半透明外观）。
- 糖果 / 玉米糖物品、玉米糖方块、棒棒糖结构（每个最多两色）、分解配方（第三期）。
- 糖果维度目前没有矿石与结构；`sugar_frost` 已经是重力方块（提前完成第三期的「糖霜下落」）。
- 生成开销：每个区块约 2~3 万个方块替换，单区块耗时明显高于原版（64 区块强制加载时出现一次
  「Can't keep up」）。后续可以考虑只在区块第一次生成时按 section 批量替换。

### 8.6 第二轮反馈调整（颜色 / 果冻 / 落地平台）

根据实机反馈做了四处修改：

1. **糖果颜色改为大片区域**。原来是逐方块哈希，看起来像彩色噪点（「太花哨」）。现在用
   **2D Voronoi 色块**：每 `36×36` 格（果冻 `26×26`）一个抖动中心，取最近中心的颜色，
   抖动限制在格子中间一半以消除细长碎片。于是地面是「这边一大片红、那边一大片蓝」，
   悬崖上则是同一颜色的竖向色柱。色块布局见 `docs/screenshots/candy-surface-colours.png`。
2. **世界生成只用鲜艳色**（红/橙/黄/黄绿/绿/青/淡蓝/蓝/紫/品红/粉 共 11 色）。白、灰、淡灰、黑、
   棕色在不还原材质里保留（可放置、可合成），但不参与地形，否则大片白色/灰色在夜里看起来像石头。
3. **果冻去掉花纹**：贴图改成单一的半透明纯色（alpha 153，即 60% 不透明），不再有浅色斑块；
   这样地表只呈现「一块半透明果冻色」，底下的糖果颜色透出来即为其质感。
4. **糖果方块改成羊毛式纯色**：之前误把「单块双色」做进了糖果方块（白底 + 彩色糖点），
   实机看起来像花纹。现在 16 色糖果/果冻都是**纯色**（各自保留一点糖面颗粒感与顶部微高光），
   「一个结构最多混两种颜色」只用于第三期的棒棒糖。
5. **到达侧 6×6 落地平台**：传送门到达位置由高度图决定，可能落在树顶、水面或半空（反馈中
   「出生在半空中」）。`CandyPortalBuilder` 现在会在门框底部那一层，把 6×6 范围内所有
   **空气/流体** 换成门框材质（`candy_rock`）；已经有实心地面的地方不动。平台方块属于已点亮
   结构的一部分，`CandyRockBlock` 的检查扩展到门框周围 ±2 格（`anyLitFrameNear`），所以平台
   不会像孤立门框那样回退，门被拆掉时会和门框一起回退成霜蜜糖。
6. **天空再压暗一档**：`CandyClientColors` 从 `(0.55, 0.29, 0.42)` 调到 `(0.40, 0.19, 0.29)`，
   生物群系 JSON 的 `sky_color` / `fog_color` 同步为 `6697034`。原值偏亮，半夜里连最暗的一层
   星空都看得见，天顶看起来像一层网格；压暗后仍然是明显的粉色夜空，但更像「夜」。

顺带的两处小改动：

- **沙子/砂砾/黏土 → 糖霜**（规格里只写了石头/草土/水）。否则海边、河床会留下原版灰褐色沙砾，
  在糖果地形里非常突兀；糖霜本来就是「白色沙状」方块，正好当糖果维度的沙。
  砂岩归入石头类 → 糖果方块。
- **地形替换改为直接写 chunk section**（`LevelChunkSection#setBlockState`），不再逐方块走
  `WorldGenRegion#setBlock`（每方块一次区块查找 + 高度图维护）。替换不改变「阻挡移动/含流体」
  属性，高度图仍然有效；该阶段还没跑光照，直接写 section 是安全的。实测 64 区块强制加载仍会
  触发一次 `Can't keep up`（约 45ms/区块），大头是原版区块生成、光照和存档，而不是这次替换。

**关于「掉进很大的横切面」**：解析生成的 region 文件（`build/tools/inspect_candy_world.py`）后，
糖果维度所有已生成区块都是 `minecraft:full`，没有任何一列的地表落在糖霜地板高度（`≤ Y6`），
也没有发现整列缺失的地形，因此不是「地形没生成出来」。当时更可能是两种情况的叠加：
到达侧被建在半空（已在 8.6.4 修复），以及走得太快时客户端区块还没收到（服务端生成跟不上），
会看到相邻区块的侧面像被切开一样。如果还能复现，麻烦给一张截图和 F3 坐标（维度 + x/y/z），
我再按具体位置排查。
