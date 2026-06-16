let currentSolution = null;
let currentWrongQuestions = [];

const questionInput = document.querySelector("#questionInput");
const solveBtn = document.querySelector("#solveBtn");
const fillExampleBtn = document.querySelector("#fillExampleBtn");
const addWrongBtn = document.querySelector("#addWrongBtn");
const refreshBtn = document.querySelector("#refreshBtn");
const analysisBtn = document.querySelector("#analysisBtn");
const practiceBtn = document.querySelector("#practiceBtn");
const subjectButtons = document.querySelectorAll(".subject-btn");
const solutionBox = document.querySelector("#solutionBox");
const wrongList = document.querySelector("#wrongList");
const analysisBox = document.querySelector("#analysisBox");
const practiceBox = document.querySelector("#practiceBox");
const questionPreview = document.querySelector("#questionPreview");
const wrongDetailModal = document.querySelector("#wrongDetailModal");
const wrongDetailBody = document.querySelector("#wrongDetailBody");
const closeWrongDetailBtn = document.querySelector("#closeWrongDetailBtn");

let currentSubject = localStorage.getItem("mathmentor_subject") || "math";

const subjectConfig = {
    math: {
        label: "数学",
        emptyWrongText: "数学错题本为空。解析数学题后可以加入这里。",
        placeholder: "输入数学题，公式可使用 LaTeX，例如：求不定积分：\\(\\int \\frac{\\ln x}{x\\sqrt{1+\\ln x}} dx\\)",
        example: "求不定积分：\\(\\int \\frac{\\ln x}{x\\sqrt{1+\\ln x}} dx\\)"
    },
    english: {
        label: "英语",
        emptyWrongText: "英语错题本为空。解析英语题后可以加入这里。",
        placeholder: "输入英语题，例如：Analyze the sentence: Although it was raining, we still went out.",
        example: "Analyze the sentence: Although it was raining, we still went out."
    }
};

const api = {
    async post(url, data = {}) {
        const response = await fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(data)
        });
        return parseResponse(response);
    },
    async put(url, data = {}) {
        const response = await fetch(url, {
            method: "PUT",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(data)
        });
        return parseResponse(response);
    },
    async get(url) {
        const response = await fetch(url);
        return parseResponse(response);
    },
    async delete(url) {
        const response = await fetch(url, {method: "DELETE"});
        if (!response.ok) {
            throw new Error(await responseMessage(response));
        }
    }
};

async function parseResponse(response) {
    if (!response.ok) {
        throw new Error(await responseMessage(response));
    }
    return response.json();
}

async function responseMessage(response) {
    const text = await response.text();
    try {
        return JSON.parse(text).message || text;
    } catch (error) {
        return text;
    }
}

function markdown(text) {
    const normalized = normalizeMathText(text || "");
    const protectedMath = protectMath(normalized);
    let html = marked.parse(protectedMath.text);
    protectedMath.items.forEach((item, index) => {
        html = html.replace(`@@MATHBLOCK${index}@@`, renderFormula(item.formula, item.display));
    });
    return html;
}

function normalizeMathText(value) {
    const text = String(value ?? "")
        .replace(/\r\n/g, "\n")
        .replace(/\\left\s*\(/g, "\\left(")
        .replace(/\\right\s*\)/g, "\\right)")
        .replace(/,\s*dt/g, "\\,dt")
        .replace(/,\s*dx/g, "\\,dx")
        .replace(/,\s*du/g, "\\,du");

    const trimmed = text.trim();
    const hasDelimiter = /\\\(|\\\[|\$/.test(trimmed);
    const isSingleLine = !trimmed.includes("\n");
    if (trimmed && isSingleLine && looksLikeFormula(trimmed) && !hasDelimiter) {
        return `$$${trimmed}$$`;
    }
    return text;
}

function protectMath(text) {
    const items = [];
    let result = text;

    const push = (formula, display) => {
        const id = items.length;
        items.push({formula: cleanupFormula(formula), display});
        return `@@MATHBLOCK${id}@@`;
    };

    result = result.replace(/\$\$([\s\S]+?)\$\$/g, (_, formula) => push(formula, true));
    result = result.replace(/\\\[([\s\S]+?)\\\]/g, (_, formula) => push(formula, true));
    result = result.replace(/\\\(([\s\S]+?)\\\)/g, (_, formula) => push(formula, false));
    result = result.replace(/\$([^$\n]+?)\$/g, (_, formula) => push(formula, false));
    result = result.replace(/\[\s*([^\]\n]*(?:\\[a-zA-Z]+|[\^_{}=+\-*/]|sqrt|frac|ln|int|sum|lim)[^\]\n]*)\s*\]/g, (_, formula) => push(formula, true));
    result = result.replace(/\(\s*([^()\n]*(?:\\[a-zA-Z]+|[\^_{}=+\-*/]|sqrt|frac|ln|int|sum|lim)[^()\n]*)\s*\)/g, (_, formula) => push(formula, false));

    if (items.length === 0 && looksLikeStandaloneFormula(result)) {
        result = push(result, true);
    }

    return {text: result, items};
}

function looksLikeStandaloneFormula(text) {
    const trimmed = text.trim();
    return !trimmed.includes("\n") && looksLikeFormula(trimmed);
}

function looksLikeFormula(text) {
    return /\\[a-zA-Z]+|[\^_{}=]/.test(text);
}

function cleanupFormula(formula) {
    return String(formula ?? "")
        .trim()
        .replace(/^\[|\]$/g, "")
        .replace(/^\(|\)$/g, "")
        .replace(/,\s*(dt|dx|du|dy)/g, "\\,$1")
        .replace(/\s+/g, " ");
}

function renderFormula(formula, display) {
    if (!window.katex) {
        return escapeHtml(formula);
    }
    try {
        return katex.renderToString(formula, {
            displayMode: display,
            throwOnError: false,
            strict: false,
            output: "htmlAndMathml"
        });
    } catch (error) {
        return escapeHtml(formula);
    }
}

function renderTextBlock(text) {
    return `<div class="markdown-body">${markdown(text)}</div>`;
}

function tags(items = [], warn = false) {
    return items.map(item => `<span class="tag ${warn ? "warn" : ""}">${escapeHtml(item)}</span>`).join("");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function renderSolution(solution) {
    solutionBox.className = "";
    solutionBox.innerHTML = `
        <article class="solution-card">
            <div class="meta-row">
                <span class="tag">${escapeHtml(subjectConfig[solution.subject || "math"]?.label || "数学")}</span>
                <span class="tag">${escapeHtml(solution.questionType || "题目")}</span>
                <span class="tag">${escapeHtml(solution.difficulty || "未标注难度")}</span>
                ${tags(solution.knowledgePoints)}
            </div>
            <div class="answer-layout">
                <section class="answer-section">
                    <div class="section-label">题目</div>
                    ${renderTextBlock(solution.question)}
                </section>
                <section class="answer-section final-answer">
                    <div class="section-label">最终答案</div>
                    ${renderTextBlock(solution.finalAnswer)}
                </section>
                <section class="answer-section">
                    <div class="section-label">答案解析</div>
                    ${renderTextBlock(solution.explanation)}
                </section>
                <section class="answer-section">
                    <div class="section-label">易错点</div>
                    <div class="meta-row">${tags(solution.commonMistakes, true)}</div>
                </section>
            </div>
        </article>
    `;
}

async function solveQuestion() {
    const question = questionInput.value.trim();
    if (!question) {
        solutionBox.textContent = "请先输入题目。";
        solutionBox.className = "empty-state";
        return;
    }
    setBusy(solveBtn, true, "解题中...");
    addWrongBtn.disabled = true;
    try {
        currentSolution = await api.post("/api/solve", {question, subject: currentSubject});
        renderSolution(currentSolution);
        addWrongBtn.disabled = false;
    } catch (error) {
        solutionBox.textContent = "解题失败：" + error.message;
        solutionBox.className = "empty-state";
    } finally {
        setBusy(solveBtn, false, "AI 解题");
    }
}

async function addWrongQuestion() {
    if (!currentSolution) {
        return;
    }
    const mistakeReason = prompt("填写错误原因，方便后续分析：", currentSubject === "math" ? "计算步骤不熟练" : "语法点不熟悉");
    setBusy(addWrongBtn, true, "保存中");
    try {
        await api.post("/api/wrong-questions", {solution: currentSolution, mistakeReason});
        await loadWrongQuestions();
        await loadAnalysis();
    } catch (error) {
        alert("加入错题本失败：" + error.message);
    } finally {
        setBusy(addWrongBtn, false, "加入错题本");
    }
}

async function loadWrongQuestions() {
    const items = await api.get(`/api/wrong-questions?subject=${currentSubject}`);
    currentWrongQuestions = items;
    if (!items.length) {
        wrongList.innerHTML = `<div class="empty-state">${subjectConfig[currentSubject].emptyWrongText}</div>`;
        return;
    }
    wrongList.innerHTML = items.map(item => `
        <article class="wrong-card" data-id="${item.id}">
            <h3>${escapeHtml(item.question)}</h3>
            <div class="meta-row">${tags(item.knowledgePoints)}${item.mastered ? '<span class="tag success">已掌握</span>' : '<span class="tag warn">待巩固</span>'}</div>
            <div class="section-label">答案</div>
            ${renderTextBlock(item.finalAnswer)}
            <div class="section-label">错因</div>
            <div>${escapeHtml(item.mistakeReason || "暂未填写")}</div>
            <div class="card-actions">
                <button class="mini-btn ${item.mastered ? "active" : ""}" data-action="master" data-id="${item.id}">${item.mastered ? "取消掌握" : "标记掌握"}</button>
                <button class="mini-btn danger" data-action="delete" data-id="${item.id}">删除</button>
            </div>
        </article>
    `).join("");
}

async function loadAnalysis() {
    setBusy(analysisBtn, true, "生成中");
    try {
        const data = await api.get(`/api/analysis?subject=${currentSubject}`);
        const statRows = Object.entries(data.knowledgeStats || {})
            .map(([point, count]) => `<span class="tag">${escapeHtml(point)}：${count}</span>`)
            .join("") || `<span class="tag">暂无数据</span>`;
        analysisBox.className = "analysis-box";
        analysisBox.innerHTML = `
            <div class="stat-grid">
                <div class="stat-item"><span class="stat-value">${data.totalWrong}</span>错题总数</div>
                <div class="stat-item"><span class="stat-value">${data.masteredCount}</span>已掌握</div>
            </div>
            <div class="section-label">知识点分布</div>
            <div class="meta-row">${statRows}</div>
            <div class="section-label">AI 建议</div>
            ${renderTextBlock(data.advice)}
            <div class="section-label">复习计划</div>
            <ol>${(data.reviewPlan || []).map(item => `<li>${escapeHtml(item)}</li>`).join("")}</ol>
        `;
    } catch (error) {
        analysisBox.textContent = "分析失败：" + error.message;
        analysisBox.className = "analysis-box empty-state";
    } finally {
        setBusy(analysisBtn, false, "生成建议");
    }
}

async function generatePractice() {
    setBusy(practiceBtn, true, "生成中");
    try {
        const items = await api.post(`/api/practice?subject=${currentSubject}`);
        practiceBox.className = "practice-list";
        practiceBox.innerHTML = items.map((item, index) => `
            <article class="practice-card">
                <h3>练习 ${index + 1}：${escapeHtml(item.question)}</h3>
                <div class="meta-row">${tags(item.knowledgePoints)}<span class="tag">${escapeHtml(item.difficulty || "中等")}</span></div>
                <div class="section-label">答案</div>
                ${renderTextBlock(item.finalAnswer)}
                <div class="section-label">解析</div>
                ${renderTextBlock(item.explanation)}
            </article>
        `).join("");
    } catch (error) {
        practiceBox.textContent = "生成失败：" + error.message;
        practiceBox.className = "practice-list empty-state";
    } finally {
        setBusy(practiceBtn, false, "生成题目");
    }
}

function setBusy(button, busy, text) {
    button.disabled = busy;
    button.textContent = text;
    button.classList.toggle("loading", busy);
}

function updateQuestionPreview() {
    if (!questionPreview) {
        return;
    }
    if (currentSubject !== "math") {
        questionPreview.hidden = true;
        return;
    }
    questionPreview.hidden = false;
    const value = questionInput.value.trim();
    questionPreview.innerHTML = `
        <div class="section-label">公式预览</div>
        ${value ? renderTextBlock(value) : '<div class="empty-state">输入数学公式后，这里会实时渲染预览。</div>'}
    `;
}

function updateSubjectView() {
    subjectButtons.forEach(button => {
        button.classList.toggle("active", button.dataset.subject === currentSubject);
    });
    questionInput.placeholder = subjectConfig[currentSubject].placeholder;
    analysisBox.textContent = `${subjectConfig[currentSubject].label}错题加入后，可自动统计薄弱知识点并生成复习建议。`;
    analysisBox.className = "analysis-box empty-state";
    practiceBox.textContent = `根据${subjectConfig[currentSubject].label}错题知识点生成相似题和解析。`;
    practiceBox.className = "practice-list empty-state";
    updateQuestionPreview();
}

function openWrongDetail(id) {
    const item = currentWrongQuestions.find(entry => entry.id === id);
    if (!item) {
        return;
    }
    wrongDetailBody.innerHTML = `
        <div class="meta-row">
            <span class="tag">${escapeHtml(subjectConfig[item.subject || "math"]?.label || "数学")}</span>
            ${tags(item.knowledgePoints)}
            ${item.mastered ? '<span class="tag success">已掌握</span>' : '<span class="tag warn">待巩固</span>'}
        </div>
        <section class="answer-section">
            <div class="section-label">题目</div>
            ${renderTextBlock(item.question)}
        </section>
        <section class="answer-section final-answer">
            <div class="section-label">答案</div>
            ${renderTextBlock(item.finalAnswer)}
        </section>
        <section class="answer-section">
            <div class="section-label">原解析</div>
            ${renderTextBlock(item.explanation)}
        </section>
        <section class="answer-section">
            <div class="section-label">错因记录</div>
            <div>${escapeHtml(item.mistakeReason || "暂未填写")}</div>
        </section>
    `;
    wrongDetailModal.hidden = false;
}

function closeWrongDetail() {
    wrongDetailModal.hidden = true;
    wrongDetailBody.innerHTML = "";
}

fillExampleBtn.addEventListener("click", () => {
    questionInput.value = subjectConfig[currentSubject].example;
    updateQuestionPreview();
});
subjectButtons.forEach(button => {
    button.addEventListener("click", () => {
        currentSubject = button.dataset.subject || "math";
        localStorage.setItem("mathmentor_subject", currentSubject);
        updateSubjectView();
        loadWrongQuestions();
    });
});
solveBtn.addEventListener("click", solveQuestion);
addWrongBtn.addEventListener("click", addWrongQuestion);
refreshBtn.addEventListener("click", loadWrongQuestions);
analysisBtn.addEventListener("click", loadAnalysis);
practiceBtn.addEventListener("click", generatePractice);
questionInput.addEventListener("input", updateQuestionPreview);
wrongList.addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");
    if (button) {
        const id = button.dataset.id;
        if (button.dataset.action === "delete") {
            await api.delete(`/api/wrong-questions/${id}`);
        }
        if (button.dataset.action === "master") {
            const mastered = !button.classList.contains("active");
            await api.put(`/api/wrong-questions/${id}`, {mastered});
        }
        await loadWrongQuestions();
        await loadAnalysis();
        return;
    }

    const card = event.target.closest(".wrong-card");
    if (card) {
        openWrongDetail(Number(card.dataset.id));
    }
});
closeWrongDetailBtn.addEventListener("click", closeWrongDetail);
wrongDetailModal.addEventListener("click", event => {
    if (event.target === wrongDetailModal) {
        closeWrongDetail();
    }
});

document.addEventListener("DOMContentLoaded", async () => {
    updateSubjectView();
    await loadWrongQuestions();
    updateQuestionPreview();
});
