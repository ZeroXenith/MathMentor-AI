# MathMentor AI

[![Java CI](https://github.com/ZeroXenith/MathMentor-AI/actions/workflows/ci.yml/badge.svg)](https://github.com/ZeroXenith/MathMentor-AI/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED.svg)](https://www.docker.com/)

MathMentor AI 是一个面向数学与英语学习场景的 AI 解题、错题诊断与强化练习系统。基于 Java 21 与 Spring Boot 3 构建，接入 DeepSeek / OpenAI-compatible 大模型，支持数学题与英语题解析、LaTeX 公式渲染、用户登录注册、错题本持久化、薄弱知识点分析和强化练习生成。

适合作为专科毕业设计、考研复试或简历中的 Java + AI 应用项目展示。

> 🆕 v0.1.0 — JWT 鉴权、API 限流、Docker 支持、GitHub Actions CI

## 截图预览

<!-- TODO: 添加截图 -->
<!-- ![主界面](docs/screenshot-main.png) -->

## 项目亮点

- 使用 Spring Boot 3 构建 RESTful API，覆盖 AI 解题、登录注册、错题本、学习分析和练习生成
- JWT 鉴权替代内存 Session，Token 7 天有效
- 接入 DeepSeek OpenAI-compatible API，可通过环境变量切换模型、网关和 API Key
- 针对数学公式显示做了专门优化，使用 LaTeX + KaTeX 直渲染，提升答案解析可读性
- Spring Data JPA 持久化用户与错题数据，默认 H2 文件数据库，支持切换 MySQL
- 密码 BCrypt 哈希存储，API 限流保护（60 次/分钟/IP）
- 前端原生 HTML/CSS/JavaScript 单页学习工作台，CDN 自动降级
- Docker 一键部署，多环境配置（dev/prod）

## 功能

- 用户注册、登录、退出（JWT 鉴权）
- 数学 / 英语题输入与 AI 自动解答
- 标准答案、解析步骤、知识点、易错点展示
- Markdown + LaTeX 数学公式实时渲染
- 错题本添加、删除、掌握状态标记
- 错题数据持久化保存
- 根据错题知识点生成个性化学习建议
- 根据错题库生成强化练习题
- API 请求限流保护

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4 |
| 持久化 | Spring Data JPA |
| 数据库 | H2 / MySQL |
| 构建 | Maven |
| 鉴权 | JJWT (HMAC-SHA256) |
| HTTP 客户端 | Java HttpClient |
| 密码加密 | BCrypt |
| 前端 | HTML / CSS / JavaScript |
| 渲染 | marked.js + KaTeX |
| 容器化 | Docker + Docker Compose |
| CI/CD | GitHub Actions |

## 项目结构

```
src/
  main/java/com/graduation/mathai/
    config/          # 配置（AiProperties, WebConfig）
    controller/      # REST 控制器
    dto/             # 数据传输对象（Records）
    interceptor/     # 拦截器（限流）
    model/           # JPA 实体
    repository/      # Spring Data 仓库
    service/         # 业务逻辑
      AiService         — 业务编排
      AiChatClient      — LLM HTTP 通信 + 重试
      PromptBuilder     — Prompt 构建
      MockDataProvider  — Mock 数据
      AuthService       — 认证 + JWT
      JwtTokenProvider  — JWT 签发/验证
      WrongQuestionService — 错题管理
    util/            # 工具类
  main/resources/
    static/          # 前端资源
    application*.yml # 多环境配置
  test/              # 单元测试
```

## 快速启动

### 方式一：Maven

```bash
mvn spring-boot:run
```

打开浏览器访问 `http://localhost:18080`

### 方式二：Docker

```bash
docker compose up -d
```

默认启用 Mock 模式，无需 API Key 即可体验所有功能。

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SERVER_PORT` | 服务端口 | `18080` |
| `AI_API_KEY` | AI API Key | — |
| `DEEPSEEK_API_KEY` | DeepSeek API Key（备选） | — |
| `OPENAI_API_KEY` | OpenAI API Key（备选） | — |
| `AI_BASE_URL` | API 网关地址 | `https://api.deepseek.com` |
| `AI_MODEL` | 模型名称 | `deepseek-v4-flash` |
| `AI_MOCK_ENABLED` | 启用 Mock 模式 | `false` |
| `JWT_SECRET` | JWT 签名密钥 | 内置默认值 |
| `DB_URL` | 数据库连接 | `jdbc:h2:file:./data/mathmentor` |
| `DB_USERNAME` | 数据库用户名 | `sa` |
| `SPRING_PROFILES_ACTIVE` | 激活配置 | — |

### 切换配置

```bash
# 开发环境（Mock 模式 + SQL 日志）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境（MySQL）
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

## 配置 DeepSeek API

```powershell
$env:DEEPSEEK_API_KEY="你的 API Key"
$env:AI_BASE_URL="https://api.deepseek.com"
$env:AI_MODEL="deepseek-v4-flash"
mvn spring-boot:run
```

项目会按顺序读取：`AI_API_KEY` → `DEEPSEEK_API_KEY` → `OPENAI_API_KEY`

## 切换到 MySQL

```sql
CREATE DATABASE mathmentor_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/mathmentor_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
mvn spring-boot:run
```

## API 接口

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | 否 | 用户注册 |
| `POST` | `/api/auth/login` | 否 | 用户登录 |
| `POST` | `/api/solve` | 否 | AI 解题 |
| `GET` | `/api/wrong-questions` | 是 | 查询错题本 |
| `POST` | `/api/wrong-questions` | 是 | 加入错题 |
| `PUT` | `/api/wrong-questions/{id}` | 是 | 更新掌握状态 |
| `DELETE` | `/api/wrong-questions/{id}` | 是 | 删除错题 |
| `GET` | `/api/analysis` | 是 | 生成学习分析 |
| `POST` | `/api/practice` | 是 | 生成强化练习 |

认证请求头：

```text
X-Auth-Token: <JWT token>
```

## 简历写法参考

> MathMentor AI：基于 Spring Boot 3 与 Java 21 开发的 AI 学习辅助系统，接入 DeepSeek 大模型，实现数学题自动解析、LaTeX 公式渲染、JWT 鉴权、错题本持久化、薄弱知识点分析与强化练习生成。项目采用 Spring Data JPA 完成数据持久化，支持 H2 本地运行与 MySQL 生产部署，内置 API 限流与 LLM 请求重试机制，支持 Docker 容器化部署及 GitHub Actions 持续集成。

## 毕设创新点参考

- 引入大语言模型完成数学题答案解析、知识点提取和易错点总结
- 基于错题本数据统计薄弱知识点，生成个性化学习建议
- 根据错题知识点自动生成强化练习题，形成"解题→订正→分析→练习"的学习闭环
- 针对数学公式展示不稳定问题，设计 LaTeX 公式规范化与 KaTeX 直渲染方案
- JWT 无状态鉴权 + API 限流，兼顾安全与性能

## 后续扩展

- PDF / Word 试卷上传解析
- 图片 OCR 和数学公式识别
- 管理员题库管理
- ECharts 学情可视化
- 试卷答案导出为 Word 或 PDF

## License

MIT
