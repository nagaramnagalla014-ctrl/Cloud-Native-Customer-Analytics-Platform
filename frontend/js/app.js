'use strict';

const REVENUE_DAYS = [
  {day:'Mon',val:152000},{day:'Tue',val:189000},{day:'Wed',val:174000},
  {day:'Thu',val:221000},{day:'Fri',val:298000},{day:'Sat',val:312000},{day:'Sun',val:188000}
];

const PRODUCTS = [
  {name:'Smart Watch Pro',revenue:'$892,400',units:10218,share:18.5},
  {name:'Wireless Earbuds X',revenue:'$641,200',units:16030,share:13.3},
  {name:'Laptop Stand Elite',revenue:'$412,800',units:5160,share:8.6},
  {name:'USB-C Hub 7-in-1',revenue:'$338,100',units:11270,share:7.0},
  {name:'Mechanical Keyboard',revenue:'$291,500',units:2915,share:6.0},
];

const CHANNELS = [
  {name:'Web',sessions:'2,841,200',conv:105124,rate:'3.7%',revenue:'$2.99M',share:62,color:'#4f8ef7'},
  {name:'Mobile App',sessions:'1,124,000',conv:36268,rate:'3.2%',revenue:'$1.16M',share:24,color:'#8b5cf6'},
  {name:'Email',sessions:'291,000',conv:12424,rate:'4.3%',revenue:'$385K',share:8,color:'#06d6c0'},
  {name:'SMS',sessions:'87,400',conv:3672,rate:'4.2%',revenue:'$115K',share:4,color:'#22d3a5'},
];

const SEGMENTS = [
  {name:'Champions',customers:'8,421',ltv:'$1,284',risk:'low',criteria:'High freq + High value + Recent purchase'},
  {name:'Loyal Customers',customers:'22,340',ltv:'$742',risk:'low',criteria:'Freq buyer, above avg order value'},
  {name:'Potential Loyalists',customers:'31,892',ltv:'$421',risk:'low',criteria:'Recent + moderate frequency'},
  {name:'At Risk',customers:'4,892',ltv:'$830',risk:'medium',criteria:'Above avg hist, not recent 90 days'},
  {name:'Cannot Lose Them',customers:'2,104',ltv:'$1,920',risk:'high',criteria:'High LTV, last purchase >180 days'},
  {name:'Churned',customers:'1,203',ltv:'$124',risk:'high',criteria:'No purchase in 365+ days'},
];

const EVENT_STATS = [
  {type:'PAGE_VIEW',count:'12,492,000',pct:100},
  {type:'CLICK',count:'4,821,000',pct:39},
  {type:'PURCHASE',count:'1,247,832',pct:10},
  {type:'CART_ADD',count:'3,104,000',pct:25},
  {type:'CART_ABANDON',count:'1,856,000',pct:15},
  {type:'SEARCH',count:'2,408,000',pct:19},
];

const REPORTS = [
  {name:'December 2024 Monthly',type:'MONTHLY',status:'complete',date:'2025-01-03'},
  {name:'Week 52 2024',type:'WEEKLY',status:'complete',date:'2024-12-31'},
  {name:'November 2024 Monthly',type:'MONTHLY',status:'complete',date:'2024-12-01'},
  {name:'Q4 2024 Custom',type:'CUSTOM',status:'complete',date:'2024-12-28'},
];

function badge(cls, label) { return `<span class="badge badge-${cls}">${label||cls}</span>`; }

function renderDashboard() {
  const maxVal = Math.max(...REVENUE_DAYS.map(d => d.val));
  document.getElementById('revenueChart').innerHTML = REVENUE_DAYS.map(d => {
    const h = Math.round((d.val / maxVal) * 140);
    const k = (d.val / 1000).toFixed(0);
    return `<div class="bar" style="height:${h}px"><div class="bar-value">$${k}K</div></div>`;
  }).join('');
  document.getElementById('revenueLabels').innerHTML = REVENUE_DAYS.map(d => `<span>${d.day}</span>`).join('');

  document.getElementById('productsBody').innerHTML = PRODUCTS.map(p => `<tr>
    <td>${p.name}</td>
    <td><strong>${p.revenue}</strong></td>
    <td>${p.units.toLocaleString()}</td>
    <td>
      <div style="display:flex;align-items:center;gap:8px">
        <div style="flex:1;height:6px;background:rgba(255,255,255,0.06);border-radius:3px;overflow:hidden">
          <div style="width:${p.share}%;height:100%;background:linear-gradient(90deg,#4f8ef7,#8b5cf6);border-radius:3px"></div>
        </div>
        <span style="font-size:12px;color:var(--muted)">${p.share}%</span>
      </div>
    </td>
  </tr>`).join('');

  document.getElementById('channelBody').innerHTML = CHANNELS.map(c => `<tr>
    <td><span style="display:inline-flex;align-items:center;gap:8px"><span style="width:10px;height:10px;border-radius:50%;background:${c.color}"></span>${c.name}</span></td>
    <td>${c.sessions}</td>
    <td>${c.conv.toLocaleString()}</td>
    <td><strong>${c.rate}</strong></td>
    <td>${c.revenue}</td>
    <td>
      <div style="display:flex;align-items:center;gap:8px">
        <div style="flex:1;height:6px;background:rgba(255,255,255,0.06);border-radius:3px;overflow:hidden">
          <div style="width:${c.share}%;height:100%;background:${c.color};border-radius:3px"></div>
        </div>
        <span style="font-size:12px;color:var(--muted)">${c.share}%</span>
      </div>
    </td>
  </tr>`).join('');
}

function renderEventBars() {
  document.getElementById('eventBars').innerHTML = EVENT_STATS.map(e => `
    <div class="rule-bar-row">
      <div class="rule-bar-label"><span>${e.type}</span><span>${e.count}</span></div>
      <div class="rule-bar-track"><div class="rule-bar-fill" style="width:${e.pct}%"></div></div>
    </div>`).join('');
}

function renderSegments() {
  document.getElementById('segmentsBody').innerHTML = SEGMENTS.map(s => `<tr>
    <td><strong>${s.name}</strong></td>
    <td>${s.customers}</td>
    <td>${s.ltv}</td>
    <td>${badge(s.risk, s.risk.toUpperCase())}</td>
    <td style="font-size:12px;color:var(--muted)">${s.criteria}</td>
  </tr>`).join('');
}

function renderReports() {
  document.getElementById('reportsBody').innerHTML = REPORTS.map(r => `<tr>
    <td>${r.name}</td>
    <td>${r.type}</td>
    <td>${badge('complete', 'COMPLETE')}</td>
    <td>${r.date}</td>
    <td><button class="gen-btn" style="padding:5px 12px;font-size:12px" onclick="downloadReport()"><i class="fas fa-download"></i> Download</button></td>
  </tr>`).join('');
}

function downloadReport() {
  showToast('Report download started (demo mode — no actual file)', 'var(--blue)');
}

function generateReport() {
  const type = document.getElementById('reportType').value;
  const start = document.getElementById('startDate').value;
  const end = document.getElementById('endDate').value;
  showToast(`${type} report queued for ${start} → ${end}. Airflow DAG triggered.`, 'var(--green)');
  REPORTS.unshift({name:`${type} Report ${start}`,type,status:'complete',date:start});
  renderReports();
}

function showToast(msg, color) {
  const t = document.getElementById('reportToast');
  if (!t) return;
  t.textContent = msg;
  t.style.display = 'block';
  t.style.color = color || 'var(--green)';
  setTimeout(() => { t.style.display = 'none'; }, 4000);
}

function showTab(tab, link) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
  document.getElementById('tab-' + tab).classList.add('active');
  if (link) link.classList.add('active');
}

renderDashboard();
renderEventBars();
renderSegments();
renderReports();
