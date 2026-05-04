# MathMentor AI

MathMentor AI 是一个面向数学学习场景的 AI 解题、错题诊断与强化练习系统。项目使用 Spring Boot 提供后端 API，使用原生 HTML/CSS/JavaScript 构建前端交互，支持数学题输入、AI 解析、Markdown 与 LaTeX 渲染、错题本管理、薄弱知识点分析和相似练习生成。

这个项目适合作为 Java 后端、AI 应用接入和前后端基础工程能力的履历项目展示。

## 项目亮点

- 基于 Spring Boot 3 构建 RESTful API，覆盖 AI 解题、错题本、学习分析和练习生成等核心流程。
- 使用 Java HttpClient 接入 OpenAI-compatible API，可通过环境变量切换模型、网关和 API Key。
- 前端使用原生 HTML/CSS/JavaScript 实现单页学习工作台，集成 marked.js 与 KaTeX 渲染 Markdown 和数学公式。
- 未配置 API Key 时提供本地演示结果，方便面试或作品集展示时快速运行。
- 错题数据采用服务层统一管理，后续可平滑扩展到 MySQL、用户系统和学习画像。

## 功能

- 数学题文本输入与示例填充
- AI 自动生成答案、解析、知识点和易错点
- Markdown + LaTeX 公式渲染
- 错题本添加、删除和掌握状态标记
- 基于错题知识点生成学习建议
- 基于错题库生成强化练习题
- API Key 缺失时返回本地模拟结果

## 技术栈

- Java 21
- Spring Boot 3.4
- Maven
- Java HttpClient
- HTML/CSS/JavaScript
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

## 配置 AI API

系统默认使用 OpenAI-compatible 接口格式。可以在 PowerShell 中配置：

```powershell
$env:AI_API_KEY="你的 API Key"
$env:AI_BASE_URL="https://api.deepseek.com/chat/completions"
$env:AI_MODEL="deepseek-v4-flash"
mvn spring-boot:run
```

如果使用其他兼容接口，只需要替换 `AI_BASE_URL` 和 `AI_MODEL`。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/solve` | AI 解题并生成结构化解析 |
| `GET` | `/api/wrong-questions` | 查询错题本 |
| `POST` | `/api/wrong-questions` | 加入错题本 |
| `PUT` | `/api/wrong-questions/{id}` | 更新错题掌握状态 |
| `DELETE` | `/api/wrong-questions/{id}` | 删除错题 |
| `GET` | `/api/analysis` | 生成错题分析和学习建议 |
| `POST` | `/api/practice` | 生成强化练习 |

## 履历写法参考

可以在简历中这样描述：

> MathMentor AI：基于 Spring Boot 3 和 Java 21 开发的 AI 数学学习助手，支持 OpenAI-compatible 模型接入、数学题解析、LaTeX 渲染、错题管理、薄弱知识点分析与强化练习生成；独立完成后端 REST API、前端单页交互和 AI 服务封装。

## 后续扩展

- MySQL 持久化错题、题目和用户信息
- 用户注册登录与 JWT 鉴权
- PDF / Word 试卷上传解析
- 图片 OCR 和数学公式识别
- 管理员题库管理
- ECharts 学情可视化
- 试卷答案导出为 Word 或 PDF

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
