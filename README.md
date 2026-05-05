# MathMentor AI

MathMentor AI 是一个面向数学学习场景的 AI 解题、错题诊断与强化练习系统。项目基于 Java 21 与 Spring Boot 3 构建，接入 DeepSeek / OpenAI-compatible 大模型，支持数学题解析、LaTeX 公式渲染、用户登录注册、错题本持久化、薄弱知识点分析和强化练习生成。

这个项目适合作为专科毕业设计，也适合作为考研复试或简历中的 Java + AI 应用项目展示。

## 项目亮点

- 使用 Spring Boot 3 构建 RESTful API，覆盖 AI 解题、登录注册、错题本、学习分析和练习生成。
- 接入 DeepSeek OpenAI-compatible API，可通过环境变量切换模型、网关和 API Key。
- 针对数学公式显示做了专门优化，使用 LaTeX + KaTeX 直渲染，提升答案解析可读性。
- 使用 Spring Data JPA 持久化用户与错题数据，默认使用 H2 文件数据库，也支持切换到 MySQL。
- 密码使用 BCrypt 哈希存储，前端通过 Token 访问个人错题数据。
- 前端使用原生 HTML/CSS/JavaScript 实现单页学习工作台，运行成本低、便于答辩演示。

## 功能

- 用户注册、登录、退出
- 数学题输入与 AI 自动解答
- 标准答案、解析步骤、知识点、易错点展示
- Markdown + LaTeX 数学公式渲染
- 错题本添加、删除、掌握状态标记
- 错题数据持久化保存
- 根据错题知识点生成学习建议
- 根据错题库生成强化练习题

## 技术栈

- Java 21
- Spring Boot 3.4
- Spring Data JPA
- H2 / MySQL
- Maven
- Java HttpClient
- BCrypt
- HTML / CSS / JavaScript
- marked.js
- KaTeX

## 快速启动

```powershell
mvn spring-boot:run
```

打开浏览器访问：

```text
http://localhost:18080
```

默认会使用本地 H2 文件数据库，数据保存在项目目录的 `data/` 下。该目录已加入 `.gitignore`，不会提交到 GitHub。

## 配置 DeepSeek API

PowerShell 示例：

```powershell
$env:DEEPSEEK_API_KEY="你的 API Key"
$env:AI_BASE_URL="https://api.deepseek.com"
$env:AI_MODEL="deepseek-v4-flash"
mvn spring-boot:run
```

项目会按顺序读取：

- `AI_API_KEY`
- `DEEPSEEK_API_KEY`
- `OPENAI_API_KEY`

`AI_BASE_URL` 可以配置为网关根地址，例如 `https://api.deepseek.com`，服务会自动补齐 `/chat/completions`。

## 切换到 MySQL

如果需要使用 MySQL，可以先创建数据库：

```sql
CREATE DATABASE mathmentor_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后在 PowerShell 中配置：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/mathmentor_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
mvn spring-boot:run
```

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/login` | 用户登录 |
| `POST` | `/api/solve` | AI 解题并生成结构化解析 |
| `GET` | `/api/wrong-questions` | 查询当前用户错题本 |
| `POST` | `/api/wrong-questions` | 加入错题本 |
| `PUT` | `/api/wrong-questions/{id}` | 更新错题掌握状态 |
| `DELETE` | `/api/wrong-questions/{id}` | 删除错题 |
| `GET` | `/api/analysis` | 生成错题分析和学习建议 |
| `POST` | `/api/practice` | 生成强化练习 |

除 `/api/solve` 外，错题相关接口需要在请求头中携带：

```text
X-Auth-Token: 登录后返回的 token
```

## 简历写法参考

> MathMentor AI：基于 Spring Boot 3 与 Java 21 开发的 AI 数学学习辅助系统，接入 DeepSeek 大模型，实现数学题自动解析、LaTeX 公式渲染、用户登录注册、错题本持久化、薄弱知识点分析与强化练习生成。项目使用 Spring Data JPA 完成数据持久化，支持 H2 本地运行与 MySQL 部署，针对数学公式展示设计了公式清洗和 KaTeX 直渲染机制。

## 毕设创新点参考

- 引入大语言模型完成数学题答案解析、知识点提取和易错点总结。
- 基于错题本数据统计学生薄弱知识点，并生成个性化学习建议。
- 根据错题知识点自动生成强化练习题，形成“解题、订正、分析、练习”的学习闭环。
- 针对数学公式展示不稳定的问题，设计 LaTeX 公式规范化与 KaTeX 直渲染方案。

## 后续扩展

- PDF / Word 试卷上传解析
- 图片 OCR 和数学公式识别
- 管理员题库管理
- ECharts 学情可视化
- 试卷答案导出为 Word 或 PDF
- JWT 鉴权和角色权限管理

## License

MIT
