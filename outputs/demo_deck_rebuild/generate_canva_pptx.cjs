const fs = require('fs');
const path = require('path');
const PptxGenJS = require('pptxgenjs');
const sharp = require('sharp');

const root = '/Users/hankhuang/Developer/action/IdeaProjects/java-easy-bank';
const out = path.join(root, 'JavaEasyBank_DemoDeck_Canva_Template.pptx');
const work = path.join(root, 'outputs/demo_deck_rebuild');
const assetDir = path.join(work, 'assets');

const pptx = new PptxGenJS();
pptx.layout = 'LAYOUT_WIDE';
pptx.author = 'Java Easy Bank';
pptx.company = '資展國際 EEIT215 第四組';
pptx.subject = 'Java Easy Bank 期末 Demo 簡報模板';
pptx.title = 'JavaEasyBank_DemoDeck_Canva_Template';
pptx.lang = 'zh-TW';
pptx.theme = {
  headFontFace: 'Noto Serif TC',
  bodyFontFace: 'Noto Sans TC',
  lang: 'zh-TW',
};
pptx.defineLayout({ name: 'CUSTOM_WIDE', width: 13.333333, height: 7.5 });
pptx.layout = 'CUSTOM_WIDE';
pptx.margin = 0;

const W = 13.333333;
const H = 7.5;
const sx = (v) => (v / 1920) * W;
const sy = (v) => (v / 1080) * H;

const C = {
  paper: 'F5F1EA',
  paper2: 'EAE4DA',
  card: 'FFF9EF',
  white: 'FFFFFF',
  ink: '2B2B2B',
  muted: '5C564E',
  faint: 'A8A199',
  border: 'D6CEC3',
  sage: '5C6B5F',
  sageDark: '3F4A42',
  sageLight: 'DFE6DC',
  stamp: 'A65A4D',
  stampLight: 'EFE1DC',
  line: 'C7BFB4',
};

const fonts = {
  body: 'Noto Sans TC',
  serif: 'Noto Serif TC',
  display: 'Cormorant Garamond',
};

const headshots = {
  '黃漢億': '/Users/hankhuang/Downloads/LINE_ALBUM_大頭照（專題簡報用）_260520_4.jpg',
  '鄭以琳': '/Users/hankhuang/Downloads/LINE_ALBUM_大頭照（專題簡報用）_260520_5.jpg',
  '邱泓翔': '/Users/hankhuang/Downloads/LINE_ALBUM_大頭照（專題簡報用）_260520_3.jpg',
  '洪世帆': '/Users/hankhuang/Downloads/LINE_ALBUM_大頭照（專題簡報用）_260520_1.jpg',
  '王昶': '/Users/hankhuang/Downloads/LINE_ALBUM_大頭照（專題簡報用）_260520_2.jpg',
};
const projectLogo = path.join(root, 'frontend/public/logo.png');

async function makeRoundPortrait(name, src) {
  const dest = path.join(assetDir, `portrait_${name}.png`);
  const meta = await sharp(src).metadata();
  const side = Math.min(meta.width, meta.height);
  const left = Math.floor((meta.width - side) / 2);
  let top = Math.floor((meta.height - side) * 0.26);
  if (top < 0) top = 0;
  if (top + side > meta.height) top = meta.height - side;
  const size = 520;
  const mask = Buffer.from(
    `<svg width="${size}" height="${size}"><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="white"/></svg>`
  );
  await sharp(src)
    .extract({ left, top, width: side, height: side })
    .resize(size, size, { fit: 'cover' })
    .composite([{ input: mask, blend: 'dest-in' }])
    .png()
    .toFile(dest);
  return dest;
}

function add(slide, type, ...args) {
  if (type === 'rect') return slide.addShape(pptx.ShapeType.rect, ...args);
  if (type === 'ellipse') return slide.addShape(pptx.ShapeType.ellipse, ...args);
  if (type === 'line') return slide.addShape(pptx.ShapeType.line, ...args);
}

function txt(slide, text, x, y, w, h, opts = {}) {
  slide.addText(text, {
    x: sx(x), y: sy(y), w: sx(w), h: sy(h),
    margin: opts.margin ?? 0,
    fontFace: opts.fontFace || fonts.body,
    fontSize: opts.size || 20,
    bold: opts.bold || false,
    italic: opts.italic || false,
    color: opts.color || C.ink,
    breakLine: false,
    fit: opts.fit || 'shrink',
    valign: opts.valign || 'mid',
    align: opts.align || 'left',
    paraSpaceAfterPt: opts.after ?? 0,
    breakLine: false,
    charSpace: opts.charSpace || 0,
    lineSpacingMultiple: opts.line || 1.08,
  });
}

function rect(slide, x, y, w, h, opts = {}) {
  add(slide, 'rect', {
    x: sx(x), y: sy(y), w: sx(w), h: sy(h),
    rectRadius: opts.radius ? sx(opts.radius) : undefined,
    fill: opts.fill ? { color: opts.fill, transparency: opts.transparency || 0 } : { color: C.card, transparency: 100 },
    line: opts.line === false ? { transparency: 100 } : { color: opts.stroke || C.border, transparency: opts.strokeT || 0, width: opts.strokeW || 1 },
    shadow: opts.shadow ? { type: 'outer', color: '6B5F50', opacity: opts.shadowOpacity || 0.12, blur: opts.shadowBlur || 1.5, angle: 45, distance: opts.shadowDistance || 1 } : undefined,
  });
}

function ellipse(slide, x, y, w, h, opts = {}) {
  add(slide, 'ellipse', {
    x: sx(x), y: sy(y), w: sx(w), h: sy(h),
    fill: opts.fill ? { color: opts.fill, transparency: opts.transparency || 0 } : { color: C.card, transparency: 100 },
    line: opts.line === false ? { transparency: 100 } : { color: opts.stroke || C.border, transparency: opts.strokeT || 0, width: opts.strokeW || 1 },
    shadow: opts.shadow ? { type: 'outer', color: '6B5F50', opacity: 0.12, blur: 1.2, angle: 45, distance: 1 } : undefined,
  });
}

function line(slide, x1, y1, x2, y2, opts = {}) {
  add(slide, 'line', {
    x: sx(x1), y: sy(y1), w: sx(x2 - x1), h: sy(y2 - y1),
    line: { color: opts.color || C.border, transparency: opts.transparency || 0, width: opts.width || 1, beginArrowType: opts.beginArrowType, endArrowType: opts.endArrowType },
  });
}

function addDecor(slide, seed = 1) {
  slide.background = { color: C.paper };
  ellipse(slide, -190, -170, 560, 560, { fill: C.sageLight, transparency: 38, line: false });
  ellipse(slide, 1510, 800, 560, 560, { fill: C.stampLight, transparency: 48, line: false });
  for (let i = 0; i < 46; i++) {
    const x = (i * 137 + seed * 41) % 1920;
    const y = (i * 73 + seed * 67) % 1080;
    line(slide, x, y, x + 72, y + (i % 2 ? -8 : 8), { color: C.border, transparency: 84, width: 0.45 });
  }
  for (let i = 0; i < 16; i++) line(slide, 90 + i * 42, 930, 90 + i * 42, 1040, { color: C.border, transparency: 75, width: 0.4 });
  for (let i = 0; i < 8; i++) line(slide, 1430, 180 + i * 26, 1770, 180 + i * 26, { color: C.line, transparency: 78, width: 0.4 });
}

function addBrand(slide, x, y, scale = 1) {
  if (fs.existsSync(projectLogo)) {
    slide.addImage({
      path: projectLogo,
      x: sx(x),
      y: sy(y - 18 * scale),
      w: sx(260 * scale),
      h: sy(222 * scale),
      transparency: 0,
    });
    return;
  }
  rect(slide, x, y + 13 * scale, 62 * scale, 38 * scale, { fill: C.sage, radius: 8 * scale, line: false });
  txt(slide, 'JEB', x + 78 * scale, y, 175 * scale, 56 * scale, { fontFace: fonts.display, size: 28 * scale, color: C.sageDark, charSpace: 2 });
  txt(slide, 'Java Easy Bank', x + 78 * scale, y + 50 * scale, 260 * scale, 24 * scale, { size: 9 * scale, bold: true, color: C.muted, charSpace: 3 });
}

function addHeader(slide, no, title, subtitle = '') {
  txt(slide, String(no).padStart(2, '0'), 116, 72, 90, 32, { fontFace: fonts.display, size: 14, color: C.stamp, charSpace: 5 });
  txt(slide, title, 116, 112, 900, 78, { fontFace: fonts.serif, size: 32, bold: true, color: C.ink, line: 0.95 });
  rect(slide, 116, 203, 136, 5, { fill: C.sage, radius: 3, line: false });
  if (subtitle) txt(slide, subtitle, 116, 226, 1120, 44, { size: 14.5, color: C.muted, line: 1.08 });
}

function addCard(slide, title, body, x, y, w, h, accent = C.sage) {
  rect(slide, x, y, w, h, { fill: C.card, stroke: C.border, radius: 28, shadow: true });
  rect(slide, x, y, 8, h, { fill: accent, radius: 4, line: false });
  txt(slide, title, x + 36, y + 30, w - 72, 44, { fontFace: fonts.serif, bold: true, size: 18, color: C.ink });
  txt(slide, body, x + 36, y + 88, w - 72, h - 108, { size: 13.3, color: C.muted, line: 1.18 });
}

function addBulletList(slide, items, x, y, w, opts = {}) {
  const gap = opts.gap || 46;
  items.forEach((item, i) => {
    ellipse(slide, x, y + i * gap + 13, 9, 9, { fill: opts.dot || C.sage, line: false });
    txt(slide, item, x + 28, y + i * gap, w - 28, gap, { size: opts.size || 14.5, color: opts.color || C.ink, line: 1.02 });
  });
}

function addChip(slide, label, x, y, w, opts = {}) {
  rect(slide, x, y, w, 48, { fill: opts.fill || C.card, stroke: opts.stroke || C.border, radius: 24 });
  txt(slide, label, x, y + 6, w, 36, { size: 11.5, bold: true, color: opts.color || C.muted, align: 'center' });
}

function addPortrait(slide, name, x, y, size, portraits) {
  ellipse(slide, x, y, size, size, { fill: C.white, stroke: C.border, strokeW: 1.2, shadow: true });
  if (portraits[name]) {
    slide.addImage({ path: portraits[name], x: sx(x + 10), y: sy(y + 10), w: sx(size - 20), h: sy(size - 20) });
  } else {
    ellipse(slide, x + size * 0.27, y + size * 0.22, size * 0.46, size * 0.46, { fill: C.sage, transparency: 25, line: false });
    txt(slide, name.slice(0, 1), x, y + size * 0.28, size, size * 0.4, { fontFace: fonts.serif, size: 36, bold: true, color: C.white, align: 'center' });
  }
}

function newSlide(no, title) {
  const slide = pptx.addSlide();
  slide.addNotes(`Java Easy Bank Demo Deck - ${String(no).padStart(2, '0')} ${title}`);
  addDecor(slide, no);
  return slide;
}

async function build() {
  fs.mkdirSync(assetDir, { recursive: true });
  const portraits = {};
  for (const [name, src] of Object.entries(headshots)) {
    if (fs.existsSync(src)) portraits[name] = await makeRoundPortrait(name, src);
  }

  let slide = newSlide(1, '封面');
  addBrand(slide, 116, 68, 0.82);
  txt(slide, '期末 Demo', 1220, 94, 240, 36, { size: 14, bold: true, color: C.stamp, charSpace: 8 });
  txt(slide, 'Java Easy Bank', 116, 282, 1030, 120, { fontFace: fonts.display, size: 55, color: C.sageDark });
  txt(slide, '讓金融流程，回到清楚與安心', 124, 414, 900, 68, { fontFace: fonts.serif, size: 28, bold: true, color: C.ink });
  txt(slide, '以 Vue 3 與 Spring Boot 建構的數位銀行平台，整合開戶、帳戶、貸款、風控與信用卡服務。從使用者體驗到後台審核流程，呈現一套可實際操作的金融產品雛形。', 126, 532, 950, 150, { size: 18, color: C.muted, line: 1.18 });
  rect(slide, 1230, 265, 470, 520, { fill: C.card, stroke: C.border, radius: 42, shadow: true, shadowBlur: 2.8, shadowDistance: 2 });
  txt(slide, '資展國際 EEIT215', 1288, 342, 360, 48, { size: 17, bold: true, color: C.muted });
  txt(slide, '第四組', 1288, 402, 260, 100, { fontFace: fonts.serif, size: 43, bold: true, color: C.sageDark });
  rect(slide, 1288, 526, 210, 4, { fill: C.stamp, radius: 2, line: false });
  txt(slide, '黃漢億 / 鄭以琳 / 邱泓翔\n洪世帆 / 王昶', 1288, 574, 360, 100, { size: 17.2, color: C.ink, line: 1.22 });
  txt(slide, '2026 Final Project Demonstration', 1288, 714, 360, 32, { fontFace: fonts.display, size: 11, color: C.faint, charSpace: 5 });

  slide = newSlide(2, '分工圖');
  addHeader(slide, 2, '分工圖', '以「使用者旅程」為軸，將五個模組串成一套完整銀行服務。');
  const members = [
    ['黃漢億', '組長 / 帳戶模組', '帳戶總覽、開戶、交易與存摺資料'],
    ['鄭以琳', '副組長 / 客戶及權限', '註冊登入、會員中心、員工與權限管理'],
    ['邱泓翔', '貸款模組', '貸款申請、案件審核、帳戶與繳費'],
    ['洪世帆', '風控模組', '授信評估、徵審資料、風險決策'],
    ['王昶', '信用卡模組', '卡別申請、開卡、帳單與回饋'],
  ];
  [[164, 352], [650, 294], [1136, 352], [408, 664], [894, 664]].forEach(([x, y], i) => {
    const m = members[i];
    rect(slide, x, y, 360, 220, { fill: C.card, stroke: C.border, radius: 30, shadow: true });
    addPortrait(slide, m[0], x + 24, y + 26, 132, portraits);
    txt(slide, m[0], x + 182, y + 44, 150, 48, { fontFace: fonts.serif, size: 21, bold: true, color: C.ink });
    txt(slide, m[1], x + 182, y + 98, 150, 56, { size: 13, bold: true, color: C.sageDark });
    txt(slide, m[2], x + 34, y + 160, 292, 48, { size: 10.5, color: C.muted, line: 1.08 });
  });
  txt(slide, '核心展示順序', 1370, 348, 270, 42, { fontFace: fonts.serif, size: 17, bold: true });
  addBulletList(slide, ['開戶與登入', '資產與帳戶操作', '貸款申辦與審核', '風控評估', '信用卡申辦與繳費'], 1370, 414, 360, { size: 14, gap: 56, dot: C.stamp });

  slide = newSlide(3, '目錄');
  addHeader(slide, 3, '目錄', 'Demo 不只是列功能，而是示範一位客戶從加入銀行到完成金融服務的完整路徑。');
  [
    ['01', '專題定位', '我們解決什麼流程痛點'],
    ['02', '技術基礎', '前後端、資料庫與 API 整合'],
    ['03', '系統架構', '前台、後台、服務層與資料層分工'],
    ['04', '模組 Demo', '五位組員依模組展示功能'],
    ['05', 'Demo Flow', '把分散功能串成一條操作劇本'],
  ].forEach((a, i) => {
    const y = 326 + i * 118;
    txt(slide, a[0], 170, y, 95, 48, { fontFace: fonts.display, size: 18, color: C.stamp, charSpace: 3 });
    txt(slide, a[1], 290, y - 4, 320, 54, { fontFace: fonts.serif, size: 24, bold: true });
    txt(slide, a[2], 650, y + 8, 650, 40, { size: 15, color: C.muted });
    line(slide, 160, y + 76, 1540, y + 76, { color: C.border, width: 0.55 });
  });
  rect(slide, 1448, 286, 250, 520, { fill: C.sageDark, stroke: C.sageDark, radius: 34, shadow: true });
  txt(slide, 'DEMO\nDECK', 1490, 340, 170, 160, { fontFace: fonts.display, size: 34, color: C.paper, align: 'center', charSpace: 4 });
  txt(slide, '13 slides\n產品介面導向', 1490, 622, 170, 80, { size: 14, color: C.paper, align: 'center', line: 1.15 });

  slide = newSlide(4, '專題介紹');
  addHeader(slide, 4, '專題介紹', 'Java Easy Bank 是一套以「數位銀行前後台協作」為核心的期末專題。');
  txt(slide, '我們選擇銀行作為題目，不是只做表單 CRUD，而是把真實金融服務中常見的三件事放進系統：使用者要能順利完成申辦，後台要能追蹤與審核，資料狀態要能在不同模組之間保持一致。', 150, 320, 840, 190, { fontFace: fonts.serif, size: 20.5, color: C.ink, line: 1.16 });
  addCard(slide, '使用者端', '從註冊、登入、開戶，到帳戶查詢、貸款申請與信用卡服務，使用者可以在同一個前台入口完成主要金融操作。', 150, 590, 480, 250, C.sage);
  addCard(slide, '後台管理', '管理者可檢視客戶、員工、申請案件與審核狀態，讓 Demo 不停在前台畫面，而是有流程閉環。', 720, 590, 480, 250, C.stamp);
  addCard(slide, '資料一致性', '以 RESTful API 連接前後端，將操作結果落入資料庫，讓 Demo 流程能被驗證與追蹤。', 1290, 590, 480, 250, C.sageDark);
  txt(slide, '專題精神', 1288, 314, 220, 40, { size: 15, bold: true, color: C.stamp, charSpace: 6 });
  txt(slide, '看得見的介面\n跑得動的流程\n查得到的資料', 1288, 370, 410, 220, { fontFace: fonts.serif, size: 31, bold: true, color: C.sageDark, line: 1.18 });

  slide = newSlide(5, '技術棧');
  addHeader(slide, 5, '技術棧', '技術選型以能支撐完整 Demo 流程為優先，而不是堆疊名詞。');
  [
    ['前端介面', 'Vue 3 / Vite / Axios', '以元件化方式建構前台與後台畫面，透過 Axios 串接 RESTful API。'],
    ['後端服務', 'Java / Spring Boot / Spring Security', '處理商業邏輯、身分驗證、授權控管與資料交換。'],
    ['資料與 ORM', 'MSSQL / Hibernate / JPA', '承接帳戶、客戶、貸款、風控與信用卡資料模型。'],
    ['協作工具', 'GitHub / IntelliJ / VS Code', '以版本控管維護多人開發節奏，降低整合成本。'],
    ['外部整合', 'Line Pay / Email / JWT', '示範金流、通知與身分驗證在金融場景中的應用。'],
  ].forEach((s, i) => {
    const x = 142 + (i % 3) * 570;
    const y = 310 + Math.floor(i / 3) * 270;
    rect(slide, x, y, 500, 218, { fill: C.card, stroke: C.border, radius: 30, shadow: true });
    txt(slide, s[0], x + 34, y + 28, 220, 42, { fontFace: fonts.serif, size: 18, bold: true });
    txt(slide, s[1], x + 34, y + 82, 410, 36, { size: 15, bold: true, color: i % 2 ? C.stamp : C.sageDark });
    txt(slide, s[2], x + 34, y + 132, 420, 62, { size: 12.4, color: C.muted, line: 1.12 });
  });

  slide = newSlide(6, '系統架構圖');
  addHeader(slide, 6, '系統架構圖', '以分層架構把畫面、API、服務邏輯與資料庫責任拆清楚。');
  [
    ['使用者 / 管理者', 'Browser / Mobile Web', '前台客戶操作與後台管理入口'],
    ['前端層', 'Vue 3 + Router + Pinia', '頁面渲染、狀態管理、表單驗證、API 呼叫'],
    ['API 交互層', 'RESTful API + JWT', '請求封裝、身份驗證、錯誤回應與權限檢查'],
    ['後端服務層', 'Spring Boot Services', '帳戶、客戶、貸款、風控、信用卡商業邏輯'],
    ['資料持久層', 'JPA / Hibernate / MSSQL', 'Repository、交易紀錄、案件狀態與審核資料'],
  ].forEach((l, i) => {
    const y = 292 + i * 116;
    rect(slide, 410, y, 1100, 78, { fill: i === 2 ? C.sageDark : C.card, stroke: i === 2 ? C.sageDark : C.border, radius: 20, shadow: true });
    txt(slide, l[0], 450, y + 16, 250, 36, { size: 14, bold: true, color: i === 2 ? C.paper : C.stamp });
    txt(slide, l[1], 740, y + 14, 340, 38, { fontFace: fonts.serif, size: 17, bold: true, color: i === 2 ? C.white : C.ink });
    txt(slide, l[2], 1110, y + 18, 360, 32, { size: 12.4, color: i === 2 ? C.paper : C.muted });
    if (i < 4) line(slide, 960, y + 84, 960, y + 116, { color: C.sage, width: 1.6, transparency: 35 });
  });
  ['帳戶', '客戶權限', '貸款', '風控', '信用卡'].forEach((t, i) => addChip(slide, t, 300 + i * 270, 884, 188, { color: C.sageDark }));
  txt(slide, '模組以 API 串接，不讓功能各自孤島化。', 1320, 878, 380, 72, { size: 14, color: C.muted, line: 1.12 });

  const modules = [
    { s: 7, t: '帳戶模組 Demo', n: '黃漢億', r: '帳戶模組', sc: '台北城市科大 / 休閒事業系', ct: '0979-390-087\nhankhuang.dev.v1@gmail.com', g: '示範客戶從開戶申請、帳戶建立，到查詢資產與交易紀錄的完整帳戶旅程。', fr: ['線上開戶申請與進度查詢', '資產總覽與帳戶明細', '臺幣轉帳與交易紀錄', '數位存摺 / 卡片式帳戶資訊'], ba: ['帳戶申請審核', '帳戶資料維護', '交易紀錄查詢', '異常狀態標記與管理'], fl: '登入 → 送出開戶申請 → 後台審核 → 前台查看帳戶 → 查詢交易明細' },
    { s: 8, t: '客戶及權限模組 Demo', n: '鄭以琳', r: '客戶及權限模組', sc: '東吳大學 / 中文系', ct: '0937-255-309\ndawnorj239@gmail.com', g: '建立安全登入與會員資料維護流程，並讓後台能管理客戶、員工與操作紀錄。', fr: ['客戶註冊、登入與登出', 'Email 驗證與忘記密碼', '會員資料與大頭照更新', 'JWT 身分驗證'], ba: ['客戶資料管理', '員工資料管理', '角色與權限設定', '系統操作日誌'], fl: '註冊 → Email 驗證 → 登入 → 更新會員資料 → 後台查核權限與日誌' },
    { s: 9, t: '貸款模組 Demo', n: '邱泓翔', r: '貸款模組', sc: '東吳大學 / 數學系', ct: '0955-332-118\nevan890909@gmail.com', g: '示範貸款從前台申請、狀態追蹤，到後台案件處理與繳費的生命週期。', fr: ['貸款方案瀏覽與申請', '申請狀態追蹤', '貸款帳戶查詢', '線上繳費功能'], ba: ['貸款申請管理', '聯繫紀錄維護', '二次填單與補件', '風控送審與帳戶管理'], fl: '選擇方案 → 填寫申請 → 後台建案 → 風控送審 → 核准後查詢與繳費' },
    { s: 10, t: '風控模組 Demo', n: '洪世帆', r: '風控模組', sc: '銘傳大學 / 資管系', ct: '0928-006-152\nhmi550843@gmail.com', g: '讓授信審核不只是按鈕通過，而是呈現資料檢核、風險評分與決策紀錄。', fr: ['申請資料補件提示', '審核進度狀態回饋', '風險結果通知', '資料授權確認'], ba: ['待審案件列表', '信用與收入資料檢核', '風險評分與審核建議', '審核結果與原因紀錄'], fl: '收到案件 → 檢核資料 → 產生風險評估 → 審核決策 → 回寫案件狀態' },
    { s: 11, t: '信用卡模組 Demo', n: '王昶', r: '信用卡模組', sc: '天津大學 / 工業工程所', ct: '0930-855-697\nlucaswang193@gmail.com', g: '展示信用卡從卡別瀏覽、線上申請、開卡，到帳單查詢與繳費的前後台流程。', fr: ['信用卡卡別瀏覽與申請', '我的卡片與開卡', '消費交易與明細查詢', '信用卡帳單繳費', '回饋金顯示'], ba: ['信用卡申請審核', '卡片狀態管理', '帳單與交易資料維護', '回饋資料查詢'], fl: '瀏覽卡別 → 送出申請 → 後台審核 → 前台開卡 → 查詢帳單並繳費' },
  ];

  modules.forEach((d) => {
    const s = newSlide(d.s, d.t);
    addHeader(s, d.s, d.t, '五張模組頁使用同一套版型，後續只需替換文字，不需重新排版。');
    rect(s, 118, 294, 500, 670, { fill: C.card, stroke: C.border, radius: 42, shadow: true });
    addPortrait(s, d.n, 256, 342, 222, portraits);
    txt(s, d.n, 168, 604, 400, 64, { fontFace: fonts.serif, size: 29, bold: true, align: 'center' });
    txt(s, d.r, 168, 676, 400, 40, { size: 16, bold: true, color: C.sageDark, align: 'center' });
    line(s, 190, 744, 546, 744, { color: C.border, width: 0.6 });
    txt(s, d.sc, 168, 774, 400, 38, { size: 13.4, color: C.muted, align: 'center' });
    txt(s, d.ct, 168, 824, 400, 70, { size: 12.5, color: C.muted, align: 'center', line: 1.05 });
    addChip(s, 'Presenter Profile', 244, 912, 250, { fill: C.paper2, color: C.sageDark });
    const x = 684;
    rect(s, x, 294, 1088, 154, { fill: C.sageDark, stroke: C.sageDark, radius: 34, shadow: true });
    txt(s, 'Demo 目標', x + 42, 326, 210, 42, { fontFace: fonts.serif, size: 17, bold: true, color: C.paper });
    txt(s, d.g, x + 250, 318, 760, 78, { size: 15.6, color: C.paper, line: 1.14 });
    rect(s, x, 492, 500, 290, { fill: C.card, stroke: C.border, radius: 30, shadow: true });
    txt(s, '前台功能', x + 36, 526, 220, 48, { fontFace: fonts.serif, size: 20, bold: true });
    addBulletList(s, d.fr.slice(0, 5), x + 42, 596, 420, { size: 13.3, gap: 38, dot: C.sage });
    rect(s, x + 548, 492, 500, 290, { fill: C.card, stroke: C.border, radius: 30, shadow: true });
    txt(s, '後台功能', x + 584, 526, 220, 48, { fontFace: fonts.serif, size: 20, bold: true });
    addBulletList(s, d.ba.slice(0, 5), x + 590, 596, 420, { size: 13.3, gap: 38, dot: C.stamp });
    rect(s, x, 828, 1088, 136, { fill: C.card, stroke: C.border, radius: 30, shadow: true });
    txt(s, 'Demo 展示點', x + 36, 858, 220, 46, { fontFace: fonts.serif, size: 18, bold: true });
    txt(s, d.fl, x + 270, 856, 760, 72, { size: 14.2, bold: true, color: C.sageDark, line: 1.1 });
  });

  slide = newSlide(12, 'Demo Flow');
  addHeader(slide, 12, 'Demo Flow', '把五個模組串成一條可現場操作的路徑，避免展示變成零散畫面切換。');
  [
    ['1', '加入銀行', '註冊、驗證、登入，建立客戶身份'],
    ['2', '開始使用', '送出開戶申請，進入帳戶總覽'],
    ['3', '金融申辦', '依需求申請貸款或信用卡'],
    ['4', '後台審核', '管理端檢視資料、補件、送風控'],
    ['5', '完成閉環', '前台查詢結果、繳費、查看明細'],
  ].forEach((s, i) => {
    const x = 146 + i * 350;
    ellipse(slide, x + 64, 342, 110, 110, { fill: i % 2 ? C.stamp : C.sage, line: false, shadow: true });
    txt(slide, s[0], x + 64, 362, 110, 70, { fontFace: fonts.display, size: 31, color: C.paper, align: 'center' });
    if (i < 4) line(slide, x + 184, 397, x + 330, 397, { color: C.sage, width: 1.9, transparency: 40 });
    rect(slide, x, 520, 242, 260, { fill: C.card, stroke: C.border, radius: 28, shadow: true });
    txt(slide, s[1], x + 28, 554, 186, 44, { fontFace: fonts.serif, size: 19, bold: true, align: 'center' });
    txt(slide, s[2], x + 30, 626, 182, 96, { size: 13.2, color: C.muted, align: 'center', line: 1.12 });
  });
  txt(slide, '現場展示建議：用一個測試客戶帳號貫穿全流程；每個模組展示前先說「這一步解決哪個流程問題」，再操作畫面。', 260, 852, 1400, 72, { fontFace: fonts.serif, size: 18, bold: true, color: C.sageDark, align: 'center', line: 1.12 });

  slide = newSlide(13, '感謝與聲明');
  addHeader(slide, 13, '感謝與聲明', '感謝老師與工作人員在專題期間提供指導、回饋與行政協助。');
  txt(slide, '特別感謝', 150, 300, 360, 76, { fontFace: fonts.serif, size: 31, bold: true });
  ['蔡昇達', '郭惠民', '李健輝', '紀宜昕', '潘麗珍', '王裕繽', '陳冠竹', '陳奕兆', '洪子敬'].forEach((t, i) => {
    const x = 150 + (i % 3) * 330;
    const y = 410 + Math.floor(i / 3) * 106;
    rect(slide, x, y, 270, 72, { fill: C.card, stroke: C.border, radius: 24, shadow: true });
    txt(slide, `${t} 老師`, x + 24, y + 18, 222, 40, { size: 16, bold: true, color: C.sageDark, align: 'center' });
  });
  rect(slide, 1150, 318, 520, 330, { fill: C.sageDark, stroke: C.sageDark, radius: 42, shadow: true });
  txt(slide, 'Thank you', 1210, 384, 400, 108, { fontFace: fonts.display, size: 45, color: C.paper, align: 'center' });
  txt(slide, 'Java Easy Bank\nFinal Demo', 1210, 514, 400, 80, { size: 16.5, color: C.paper, align: 'center', line: 1.1 });
  txt(slide, '本專題僅供資展國際期末專題展示與教學用途，畫面、資料與流程皆為 Demo 情境，不涉及任何商業金融服務或真實交易。', 184, 828, 1480, 86, { size: 16.2, color: C.muted, align: 'center', line: 1.15 });
  txt(slide, 'EEIT215 第四組', 790, 930, 340, 38, { size: 14, bold: true, color: C.stamp, charSpace: 4, align: 'center' });

  await pptx.writeFile({ fileName: out });
  console.log(out);
}

build().catch((err) => {
  console.error(err);
  process.exit(1);
});
