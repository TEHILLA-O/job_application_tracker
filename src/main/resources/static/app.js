const STATUSES = ["SAVED", "APPLIED", "ASSESSMENT", "INTERVIEW", "OFFER", "ACCEPTED", "REJECTED", "WITHDRAWN"];
const SOURCES = ["LINKEDIN", "INDEED", "COMPANY_SITE", "REFERRAL", "RECRUITER", "GITHUB", "OTHER"];
const PRIORITIES = ["LOW", "MEDIUM", "HIGH"];

const state = {
    view: "board",
    items: [],
    stats: null,
    selectedId: null,
};

const $ = (id) => document.getElementById(id);

function fillSelect(select, values, includeBlank) {
    select.innerHTML = includeBlank ? `<option value="">${includeBlank}</option>` : "";
    values.forEach((value) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label(value);
        select.appendChild(option);
    });
}

function label(value) {
    return value.replaceAll("_", " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

function toast(message, kind = "ok") {
    const el = document.createElement("div");
    el.className = `toast ${kind}`;
    el.textContent = message;
    $("toasts").appendChild(el);
    setTimeout(() => el.remove(), 4200);
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options,
    });
    if (response.status === 204) return null;
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const detail = data?.message || data?.error || "Request failed";
        throw Object.assign(new Error(detail), { data });
    }
    return data;
}

function queryString() {
    const params = new URLSearchParams({ size: "100", sort: "appliedDate,desc" });
    const q = $("search").value.trim();
    const status = $("filter-status").value;
    const source = $("filter-source").value;
    const priority = $("filter-priority").value;
    if (q) params.set("q", q);
    if (status) params.set("status", status);
    if (source) params.set("source", source);
    if (priority) params.set("priority", priority);
    if ($("filter-followup").checked) params.set("followUpDue", "true");
    return params.toString();
}

async function refresh() {
    const [page, stats] = await Promise.all([
        api(`/api/applications?${queryString()}`),
        api("/api/applications/stats"),
    ]);
    state.items = page.content;
    state.stats = stats;
    render();
}

function render() {
    renderStats();
    if (state.view === "board") renderBoard();
    if (state.view === "table") renderTable();
    if (state.view === "insights") renderInsights();
}

function renderStats() {
    const s = state.stats;
    if (!s) return;
    $("stats").innerHTML = [
        ["In pipeline", s.total],
        ["Active", s.active],
        ["Interviews %", s.interviewRatePercent],
        ["Offers", s.offers],
        ["Accepted", s.accepted],
        ["Avg days open", s.averageDaysOpen],
    ].map(([k, v]) => `<article class="stat"><span>${k}</span><strong>${v}</strong></article>`).join("");
}

function renderBoard() {
    const workspace = $("workspace");
    workspace.innerHTML = `<div class="board">${STATUSES.map((status) => {
        const cards = state.items.filter((item) => item.status === status).map(cardHtml).join("");
        return `<section class="column" data-status="${status}">
            <h3>${label(status)} <span>${state.items.filter((i) => i.status === status).length}</span></h3>
            ${cards || `<p class="empty">Drop a card here.</p>`}
        </section>`;
    }).join("")}</div>`;

    workspace.querySelectorAll(".card").forEach((card) => {
        card.addEventListener("dragstart", (event) => {
            event.dataTransfer.setData("text/plain", card.dataset.id);
        });
        card.addEventListener("click", () => openDrawer(Number(card.dataset.id)));
    });
    workspace.querySelectorAll(".column").forEach((column) => {
        column.addEventListener("dragover", (event) => {
            event.preventDefault();
            column.classList.add("is-over");
        });
        column.addEventListener("dragleave", () => column.classList.remove("is-over"));
        column.addEventListener("drop", async (event) => {
            event.preventDefault();
            column.classList.remove("is-over");
            const id = event.dataTransfer.getData("text/plain");
            try {
                await api(`/api/applications/${id}/status`, {
                    method: "PATCH",
                    body: JSON.stringify({ status: column.dataset.status }),
                });
                toast(`Moved to ${label(column.dataset.status)}`);
                await refresh();
            } catch (error) {
                const allowed = error.data?.allowed ? ` Allowed: ${error.data.allowed.join(", ")}` : "";
                toast(error.message + allowed, "error");
            }
        });
    });
}

function cardHtml(item) {
    const chips = [
        `<span class="chip">${label(item.source)}</span>`,
        item.priority === "HIGH" ? `<span class="chip high">High</span>` : "",
        item.followUpDue ? `<span class="chip due">Follow-up due</span>` : "",
        item.location ? `<span class="chip">${item.location}</span>` : "",
    ].join("");
    return `<article class="card" draggable="true" data-id="${item.id}">
        <h4>${escapeHtml(item.company)}</h4>
        <p>${escapeHtml(item.role)}</p>
        <div class="meta">${chips}</div>
    </article>`;
}

function renderTable() {
    if (!state.items.length) {
        $("workspace").innerHTML = `<p class="empty">No applications match these filters.</p>`;
        return;
    }
    $("workspace").innerHTML = `<div class="table-wrap"><table>
        <thead><tr>
            <th>Company</th><th>Role</th><th>Status</th><th>Priority</th>
            <th>Applied</th><th>Follow-up</th><th>Days</th>
        </tr></thead>
        <tbody>${state.items.map((item) => `<tr data-id="${item.id}">
            <td>${escapeHtml(item.company)}</td>
            <td>${escapeHtml(item.role)}</td>
            <td>${label(item.status)}</td>
            <td>${label(item.priority)}</td>
            <td>${item.appliedDate}</td>
            <td>${item.nextFollowUp || "—"}</td>
            <td>${item.daysOpen}</td>
        </tr>`).join("")}</tbody>
    </table></div>`;
    $("workspace").querySelectorAll("tr[data-id]").forEach((row) => {
        row.addEventListener("click", () => openDrawer(Number(row.dataset.id)));
    });
}

function renderInsights() {
    const s = state.stats;
    if (!s) return;
    const maxFunnel = Math.max(1, ...s.funnel.map((step) => step.count));
    const followUps = s.upcomingFollowUps.map((item) =>
        `<li><strong>${escapeHtml(item.company)}</strong> · ${escapeHtml(item.role)}
         <div class="meta"><span class="chip ${item.overdue ? "due" : ""}">${item.nextFollowUp}</span></div></li>`
    ).join("") || "<li>No follow-ups scheduled.</li>";
    $("workspace").innerHTML = `<div class="insights">
        <section class="panel">
            <h3>Conversion funnel</h3>
            ${s.funnel.map((step) => `<div class="bar">
                <span>${label(step.stage)}</span>
                <i style="width:${Math.round((step.count / maxFunnel) * 100)}%"></i>
                <b>${step.count}</b>
            </div>`).join("")}
        </section>
        <section class="panel">
            <h3>Upcoming follow-ups</h3>
            <ol id="timeline">${followUps}</ol>
        </section>
        <section class="panel">
            <h3>Sources</h3>
            ${Object.entries(s.bySource).map(([key, count]) => `<div class="bar">
                <span>${label(key)}</span><i style="width:${Math.round((count / Math.max(1, s.total)) * 100)}%"></i><b>${count}</b>
            </div>`).join("")}
        </section>
        <section class="panel">
            <h3>Top companies</h3>
            ${s.topCompanies.map((row) => `<div class="bar">
                <span>${escapeHtml(row.company)}</span><i style="width:${Math.round((row.count / Math.max(1, s.total)) * 100)}%"></i><b>${row.count}</b>
            </div>`).join("") || "<p>No companies yet.</p>"}
        </section>
    </div>`;
}

function formPayload(form) {
    const data = new FormData(form);
    const num = (name) => {
        const value = data.get(name);
        return value ? Number(value) : null;
    };
    const tags = String(data.get("tags") || "")
        .split(",")
        .map((tag) => tag.trim())
        .filter(Boolean);
    return {
        company: data.get("company"),
        role: data.get("role"),
        status: data.get("status"),
        appliedDate: data.get("appliedDate"),
        location: data.get("location") || null,
        salaryMin: num("salaryMin"),
        salaryMax: num("salaryMax"),
        notes: data.get("notes") || null,
        source: data.get("source") || "OTHER",
        jobUrl: data.get("jobUrl") || null,
        recruiterName: data.get("recruiterName") || null,
        recruiterEmail: data.get("recruiterEmail") || null,
        priority: data.get("priority") || "MEDIUM",
        nextFollowUp: data.get("nextFollowUp") || null,
        tags,
    };
}

function fillForm(item) {
    const form = $("app-form");
    form.reset();
    if (!item) {
        form.status.value = "SAVED";
        form.source.value = "OTHER";
        form.priority.value = "MEDIUM";
        form.appliedDate.value = new Date().toISOString().slice(0, 10);
        return;
    }
    Object.entries({
        company: item.company,
        role: item.role,
        status: item.status,
        appliedDate: item.appliedDate,
        location: item.location || "",
        source: item.source,
        priority: item.priority,
        nextFollowUp: item.nextFollowUp || "",
        salaryMin: item.salaryMin ?? "",
        salaryMax: item.salaryMax ?? "",
        jobUrl: item.jobUrl || "",
        recruiterName: item.recruiterName || "",
        recruiterEmail: item.recruiterEmail || "",
        tags: (item.tags || []).join(", "),
        notes: item.notes || "",
    }).forEach(([key, value]) => {
        form[key].value = value;
    });
}

function renderTimeline(events) {
    $("timeline").innerHTML = events.map((event) => `<li>
        <strong>${label(event.type)}</strong>
        <time>${new Date(event.createdAt).toLocaleString()}</time>
        <p>${escapeHtml(event.message || "")}</p>
    </li>`).join("") || "<li>No events yet.</li>";
}

async function openDrawer(id) {
    state.selectedId = id || null;
    $("drawer").hidden = false;
    $("delete-btn").hidden = !id;
    $("timeline-wrap").hidden = !id;
    $("drawer-title").textContent = id ? "Edit application" : "New application";
    $("drawer-kicker").textContent = id ? "Open record" : "Create";
    if (!id) {
        fillForm(null);
        return;
    }
    const detail = await api(`/api/applications/${id}`);
    fillForm(detail.application);
    renderTimeline(detail.events);
}

function closeDrawer() {
    $("drawer").hidden = true;
    state.selectedId = null;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function bind() {
    fillSelect($("filter-status"), STATUSES, "All stages");
    fillSelect($("filter-source"), SOURCES, "All sources");
    fillSelect($("filter-priority"), PRIORITIES, "All priorities");
    fillSelect(document.querySelector("#app-form [name=status]"), STATUSES);
    fillSelect(document.querySelector("#app-form [name=source]"), SOURCES);
    fillSelect(document.querySelector("#app-form [name=priority]"), PRIORITIES);

    document.querySelectorAll(".view-btn").forEach((button) => {
        button.addEventListener("click", () => {
            state.view = button.dataset.view;
            document.querySelectorAll(".view-btn").forEach((b) => b.classList.toggle("is-active", b === button));
            render();
        });
    });

    ["search", "filter-status", "filter-source", "filter-priority", "filter-followup"].forEach((id) => {
        $(id).addEventListener("input", () => refresh().catch((error) => toast(error.message, "error")));
        $(id).addEventListener("change", () => refresh().catch((error) => toast(error.message, "error")));
    });

    $("new-btn").addEventListener("click", () => openDrawer(null));
    $("close-drawer").addEventListener("click", closeDrawer);
    $("drawer").addEventListener("click", (event) => {
        if (event.target === $("drawer")) closeDrawer();
    });

    $("app-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const payload = formPayload(event.target);
        try {
            if (state.selectedId) {
                await api(`/api/applications/${state.selectedId}`, { method: "PUT", body: JSON.stringify(payload) });
                toast("Application updated");
            } else {
                await api("/api/applications", { method: "POST", body: JSON.stringify(payload) });
                toast("Application created");
            }
            closeDrawer();
            await refresh();
        } catch (error) {
            toast(error.message, "error");
        }
    });

    $("delete-btn").addEventListener("click", async () => {
        if (!state.selectedId || !confirm("Delete this application?")) return;
        await api(`/api/applications/${state.selectedId}`, { method: "DELETE" });
        toast("Application deleted");
        closeDrawer();
        await refresh();
    });

    $("note-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const message = new FormData(event.target).get("message");
        try {
            const detail = await api(`/api/applications/${state.selectedId}/notes`, {
                method: "POST",
                body: JSON.stringify({ message }),
            });
            event.target.reset();
            renderTimeline(detail.events);
            toast("Note added");
            await refresh();
        } catch (error) {
            toast(error.message, "error");
        }
    });
}

bind();
refresh().catch((error) => toast(error.message, "error"));
