# 数学 AI 试卷解析与错题强化系统

这是一个 Java 毕设基础版本，使用 Spring Boot 实现后端接口，并用原生 HTML/CSS/JavaScript 完成前端页面。系统支持数学题输入、AI 解题、LaTeX 公式渲染、错题本、错题分析和强化练习生成。

## 功能

- 数学题文本输入
- AI 自动生成答案、解析、知识点和易错点
- Markdown + LaTeX 公式渲染
- 错题本添加、删除、掌握标记
- 根据错题知识点生成学习建议
- 根据错题库生成强化练习题
- 未配置 API Key 时提供本地演示结果

## 技术栈

- Java 21
- Spring Boot 3.4
- Maven
- Java HttpClient
- HTML/CSS/JavaScript
- marked.js
- KaTeX

## 启动

```powershell
mvn spring-boot:run
```

打开：

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

- `POST /api/solve`：AI 解题
- `GET /api/wrong-questions`：查询错题本
- `POST /api/wrong-questions`：加入错题本
- `PUT /api/wrong-questions/{id}`：更新错题状态
- `DELETE /api/wrong-questions/{id}`：删除错题
- `GET /api/analysis`：生成错题分析
- `POST /api/practice`：生成强化练习

## 后续扩展

- MySQL 持久化错题、题目和用户信息
- 用户登录注册与 JWT 鉴权
- PDF / Word 试卷上传解析
- 图片 OCR 和数学公式识别
- 管理员题库管理
- ECharts 学情可视化
- 试卷答案导出为 Word 或 PDF
