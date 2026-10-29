# Bedrock 模型接入说明

模组内置了一个精简的 Bedrock 模型加载器（**不依赖 YSM**），可以把 `*.geo.json` 几何和
`*.animation.json` 动画直接用在模组生物上。

## 文件放哪

```
src/main/resources/assets/myriadbloom/bedrock/
  <name>.geo.json          # 必须，几何
  <name>.animation.json    # 可选，动画（可省略）
```

贴图仍然用实体自己的贴图（`textures/entity/<name>.png`），通过渲染器的 `getTextureLocation` 指定。

## 目前接入的生物

| 生物 | name | 行为 |
|---|---|---|
| 极冰聚合物 | `glacial_conglomeration` | 有 `*.geo.json` 时用 Bedrock 模型，否则回退到内置方块模型 |
| 始源人 / 雪兽 | — | 尚未接入，按下面的步骤可以加 |

接入一个新生物：

1. 放 `assets/myriadbloom/bedrock/<name>.geo.json`（+ `animation.json`）
2. 在渲染器的构造函数里把模型换成：

```java
BedrockModels.Entry entry = BedrockModels.get("<name>");
super(context, entry != null
        ? new BedrockEntityModel<>(entry)
        : new 原来的模型(context.bakeLayer(...)), 阴影半径);
```

## 坐标与骨骼约定

- 单位：**16 = 1 方块**，`+Y 向上`，`pivot` / `origin` 以**脚底 y = 0** 为基准
- `pivot` 使用**绝对坐标**（Bedrock 原生行为）。加载器在烘焙时会**把所有骨骼摊平到根节点**，
  因为 vanilla 的 `ModelPart` 在嵌套空骨骼时会把子骨骼丢掉（实测）
- 因此**父子关系目前不影响动画继承**：子骨骼不会跟随父骨骼运动。如果你的模型依赖
  「手臂子骨骼跟随肩膀」，需要把这段动画直接写在每个骨骼上，或等后续版本补继承
- 转换公式：`y_java = 24 - pivotY`，方块 `y = pivotY - originY - sizeY`

## 动画支持范围

支持：
- `bones.<bone>.rotation / position / scale`
- `{ "0.5": [x, y, z] }` 关键帧（线性插值）
- `vector` 常量写法、`animation_length`、`loop`
- 关键帧里写**数字字符串**的 Molang 子集：`math.sin/cos/abs/pi`、`query.anim_time`、
  `query.life_time`、四则运算与括号

暂不支持：
- 逐面 UV（`uv` 写成对象时只取第一面的偏移）
- 动画混合（idle/move/attack 之间是**硬切**，不是渐变）
- 骨骼父子动画继承
- 完整的 Molang（变量赋值、`v.*`、`query.*` 的其它查询、`math.min/max` 等）

## 选择哪段动画

`BedrockEntityModel` 按以下顺序挑动画（按名字包含的关键字匹配）：

1. 手臂挥舞中 → `attack` / `slam` / `punch` / `swing`
2. 正在移动 → `walk` / `move` / `run`
3. 其它 → `idle` / `rest` / `breath`

所以动画名字里请带上这些关键字，例如 `animation.xxx.idle`、`animation.xxx.attack`。

## 已知限制

- 模型缓存在首次加载后不会自动刷新：改完 JSON 需要重启客户端
- 大型模型（几十个骨骼 + 每骨骼多方块）在烘焙时会一次性建立 `ModelPart` 树，内存占用与方块数成正比
- 每个 `BedrockEntityModel` 实例共享同一份烘焙结果（按 name 缓存），不会重复烘焙

## 调试

模型加载成功时日志会打印：

```
Bedrock model glacial_conglomeration loaded: 3 bones, 2 animations
```

找不到几何文件时打印 warn 并回退到内置模型。
