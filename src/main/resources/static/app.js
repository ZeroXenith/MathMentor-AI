let currentSolution = null;

const questionInput = document.querySelector("#questionInput");
const solveBtn = document.querySelector("#solveBtn");
const fillExampleBtn = document.querySelector("#fillExampleBtn");
const addWrongBtn = document.querySelector("#addWrongBtn");
const refreshBtn = document.querySelector("#refreshBtn");
const analysisBtn = document.querySelector("#analysisBtn");
const practiceBtn = document.querySelector("#practiceBtn");
const solutionBox = document.querySelector("#solutionBox");
const wrongList = document.querySelector("#wrongList");
const analysisBox = document.querySelector("#analysisBox");
const practiceBox = document.querySelector("#practiceBox");

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

function renderMath(container = document.body) {
    if (!window.renderMathInElement) {
        return;
    }
    renderMathInElement(container, {
        delimiters: [
            {left: "$$", right: "$$", display: true},
            {left: "\\[", right: "\\]", display: true},
            {left: "\\(", right: "\\)", display: false}
        ],
        throwOnError: false
    });
}

function markdown(text) {
    return marked.parse(text || "");
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
                <span class="tag">${escapeHtml(solution.questionType || "数学题")}</span>
                <span class="tag">${escapeHtml(solution.difficulty || "未标注难度")}</span>
                ${tags(solution.knowledgePoints)}
            </div>
            <div class="section-label">题目</div>
            <div class="markdown-body">${markdown(solution.question)}</div>
            <div class="section-label">最终答案</div>
            <div class="markdown-body">${markdown(solution.finalAnswer)}</div>
            <div class="section-label">解析</div>
            <div class="markdown-body">${markdown(solution.explanation)}</div>
            <div class="section-label">易错点</div>
            <div class="meta-row">${tags(solution.commonMistakes, true)}</div>
        </article>
    `;
    renderMath(solutionBox);
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
        currentSolution = await api.post("/api/solve", {question});
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
    const mistakeReason = prompt("填写错误原因，方便后续分析：", "计算步骤不熟练");
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
    const items = await api.get("/api/wrong-questions");
    if (!items.length) {
        wrongList.innerHTML = `<div class="empty-state">错题本为空。解析题目后可以加入这里。</div>`;
        return;
    }
    wrongList.innerHTML = items.map(item => `
        <article class="wrong-card">
            <h3>${escapeHtml(item.question)}</h3>
            <div class="meta-row">${tags(item.knowledgePoints)}${item.mastered ? '<span class="tag">已掌握</span>' : '<span class="tag warn">待巩固</span>'}</div>
            <div class="section-label">答案</div>
            <div class="markdown-body">${markdown(item.finalAnswer)}</div>
            <div class="section-label">错因</div>
            <div>${escapeHtml(item.mistakeReason || "暂未填写")}</div>
            <div class="card-actions">
                <button class="mini-btn ${item.mastered ? "active" : ""}" data-action="master" data-id="${item.id}">${item.mastered ? "取消掌握" : "标记掌握"}</button>
                <button class="mini-btn danger" data-action="delete" data-id="${item.id}">删除</button>
            </div>
        </article>
    `).join("");
    renderMath(wrongList);
}

async function loadAnalysis() {
    setBusy(analysisBtn, true, "生成中");
    try {
        const data = await api.get("/api/analysis");
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
            <div class="markdown-body">${markdown(data.advice)}</div>
            <div class="section-label">复习计划</div>
            <ol>${(data.reviewPlan || []).map(item => `<li>${escapeHtml(item)}</li>`).join("")}</ol>
        `;
        renderMath(analysisBox);
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
        const items = await api.post("/api/practice");
        practiceBox.className = "practice-list";
        practiceBox.innerHTML = items.map((item, index) => `
            <article class="practice-card">
                <h3>练习 ${index + 1}：${escapeHtml(item.question)}</h3>
                <div class="meta-row">${tags(item.knowledgePoints)}<span class="tag">${escapeHtml(item.difficulty || "中等")}</span></div>
                <div class="section-label">答案</div>
                <div class="markdown-body">${markdown(item.finalAnswer)}</div>
                <div class="section-label">解析</div>
                <div class="markdown-body">${markdown(item.explanation)}</div>
            </article>
        `).join("");
        renderMath(practiceBox);
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

fillExampleBtn.addEventListener("click", () => {
    questionInput.value = "求方程 \\(x^2-5x+6=0\\) 的解，并写出完整解题步骤。";
});
solveBtn.addEventListener("click", solveQuestion);
addWrongBtn.addEventListener("click", addWrongQuestion);
refreshBtn.addEventListener("click", loadWrongQuestions);
analysisBtn.addEventListener("click", loadAnalysis);
practiceBtn.addEventListener("click", generatePractice);
wrongList.addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }
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
});

document.addEventListener("DOMContentLoaded", async () => {
    await loadWrongQuestions();
    renderMath(document.body);
});
