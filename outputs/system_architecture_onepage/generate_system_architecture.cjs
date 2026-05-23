const fs = require('fs');
const path = require('path');
const PptxGenJS = require('pptxgenjs');

const root = '/Users/hankhuang/Developer/action/IdeaProjects/java-easy-bank';
const out = path.join(root, 'JavaEasyBank_SystemArchitecture_OnePage.pptx');
const logo = path.join(root, 'frontend/public/logo.png');

const pptx = new PptxGenJS();
pptx.defineLayout({ name: 'CUSTOM_WIDE', width: 13.333333, height: 7.5 });
pptx.layout = 'CUSTOM_WIDE';
pptx.author = 'Java Easy Bank';
pptx.company = '資展國際 EEIT215 第四組';
pptx.subject = 'Java Easy Bank 系統架構圖';
pptx.title = 'JavaEasyBank_SystemArchitecture_OnePage';
pptx.lang = 'zh-TW';
pptx.theme = {
  headFontFace: 'Noto Serif TC',
  bodyFontFace: 'Noto Sans TC',
  lang: 'zh-TW',
};

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
  sageSoft: 'DFE6DC',
  stamp: 'A65A4D',
  stampSoft: 'EFE1DC',
  goldSoft: 'DED3BF',
};

const fonts = {
  body: 'Noto Sans TC',
  serif: 'Noto Serif TC',
  display: 'Cormorant Garamond',
};

function txt(slide, text, x, y, w, h, opts = {}) {
  slide.addText(text, {
    x: sx(x), y: sy(y), w: sx(w), h: sy(h),
    margin: opts.margin ?? 0,
    fontFace: opts.fontFace || fonts.body,
    fontSize: opts.size || 14,
    bold: opts.bold || false,
    color: opts.color || C.ink,
    fit: 'shrink',
    valign: opts.valign || 'mid',
    align: opts.align || 'left',
    lineSpacingMultiple: opts.line || 1.05,
    charSpace: opts.charSpace || 0,
  });
}

function rect(slide, x, y, w, h, opts = {}) {
  slide.addShape(pptx.ShapeType.rect, {
    x: sx(x), y: sy(y), w: sx(w), h: sy(h),
    rectRadius: opts.radius ? sx(opts.radius) : undefined,
    fill: opts.fill ? { color: opts.fill, transparency: opts.transparency || 0 } : { color: C.card, transparency: 100 },
    line: opts.line === false
      ? { transparency: 100 }
      : { color: opts.stroke || C.border, transparency: opts.strokeT || 0, width: opts.strokeW || 0.7 },
    shadow: opts.shadow
      ? { type: 'outer', color: '6B5F50', opacity: 0.11, blur: 1.2, angle: 45, distance: 0.8 }
      : undefined,
  });
}

function ellipse(slide, x, y, w, h, opts = {}) {
  slide.addShape(pptx.ShapeType.ellipse, {
    x: sx(x), y: sy(y), w: sx(w), h: sy(h),
    fill: opts.fill ? { color: opts.fill, transparency: opts.transparency || 0 } : { color: C.card, transparency: 100 },
    line: opts.line === false
      ? { transparency: 100 }
      : { color: opts.stroke || C.border, transparency: opts.strokeT || 0, width: opts.strokeW || 0.7 },
  });
}

function line(slide, x1, y1, x2, y2, opts = {}) {
  slide.addShape(pptx.ShapeType.line, {
    x: sx(x1), y: sy(y1), w: sx(x2 - x1), h: sy(y2 - y1),
    line: {
      color: opts.color || C.sage,
      transparency: opts.transparency || 0,
      width: opts.width || 1.2,
      beginArrowType: opts.beginArrowType,
      endArrowType: opts.endArrowType || 'triangle',
    },
  });
}

function addPaperDecor(slide) {
  slide.background = { color: C.paper };
  ellipse(slide, -180, -160, 530, 530, { fill: C.sageSoft, transparency: 44, line: false });
  ellipse(slide, 1510, 770, 560, 560, { fill: C.stampSoft, transparency: 47, line: false });
  for (let i = 0; i < 42; i++) {
    const x = (i * 137 + 41) % 1920;
    const y = (i * 73 + 67) % 1080;
    slide.addShape(pptx.ShapeType.line, {
      x: sx(x), y: sy(y), w: sx(74), h: sy(i % 2 ? -8 : 8),
      line: { color: C.border, transparency: 86, width: 0.35 },
    });
  }
  for (let i = 0; i < 11; i++) {
    slide.addShape(pptx.ShapeType.line, {
      x: sx(92 + i * 42), y: sy(936), w: sx(0), h: sy(92),
      line: { color: C.border, transparency: 78, width: 0.3 },
    });
  }
}

function chip(slide, label, x, y, w, opts = {}) {
  rect(slide, x, y, w, 42, {
    fill: opts.fill || C.white,
    stroke: opts.stroke || C.border,
    radius: 21,
    strokeW: 0.55,
  });
  txt(slide, label, x, y + 7, w, 28, {
    size: opts.size || 10.5,
    bold: true,
    color: opts.color || C.muted,
    align: 'center',
  });
}

function layerHeader(slide, label, x, y, color = C.sageDark) {
  rect(slide, x, y, 162, 42, { fill: color, stroke: color, radius: 21 });
  txt(slide, label, x, y + 7, 162, 28, { size: 11.5, bold: true, color: C.paper, align: 'center' });
}

function box(slide, title, subtitle, x, y, w, h, opts = {}) {
  rect(slide, x, y, w, h, {
    fill: opts.fill || C.card,
    stroke: opts.stroke || C.border,
    radius: opts.radius || 22,
    shadow: opts.shadow !== false,
    strokeW: opts.strokeW || 0.65,
  });
  if (opts.accent) rect(slide, x, y, 7, h, { fill: opts.accent, radius: 4, line: false });
  txt(slide, title, x + 22, y + 17, w - 44, 32, {
    fontFace: fonts.serif,
    size: opts.titleSize || 15,
    bold: true,
    color: opts.titleColor || C.ink,
  });
  if (subtitle) txt(slide, subtitle, x + 22, y + 55, w - 44, h - 65, {
    size: opts.subSize || 10.6,
    color: opts.subColor || C.muted,
    line: 1.12,
  });
}

const slide = pptx.addSlide();
slide.addNotes('Java Easy Bank 系統架構圖：使用者端與管理端經 Vue 前端、Axios/JWT、Spring Boot 分層服務、JPA Repository 寫入 MSSQL，並整合 Email 與 Line Pay。');
addPaperDecor(slide);

if (fs.existsSync(logo)) {
  slide.addImage({ path: logo, x: sx(1602), y: sy(52), w: sx(132), h: sy(113), transparency: 4 });
}

txt(slide, '系統架構圖', 116, 96, 470, 68, {
  fontFace: fonts.serif,
  size: 31,
  bold: true,
  color: C.ink,
});
rect(slide, 116, 177, 136, 5, { fill: C.sage, radius: 3, line: false });
txt(slide, 'Java Easy Bank 以 Vue 3 前台/後台作為操作入口，透過 RESTful API 與 JWT 驗證進入 Spring Boot 後端，再由 Service 模組封裝金融流程並寫入 MSSQL。', 116, 204, 1230, 46, {
  size: 13.8,
  color: C.muted,
  line: 1.08,
});

// Outer architecture canvas
rect(slide, 100, 300, 1720, 626, {
  fill: 'FFFFFF',
  transparency: 14,
  stroke: C.border,
  radius: 34,
  shadow: true,
});

// Layer labels
layerHeader(slide, 'Client', 154, 330, C.stamp);
layerHeader(slide, 'API / Security', 154, 476, C.sageDark);
layerHeader(slide, 'Backend', 154, 618, C.sageDark);
layerHeader(slide, 'Persistence', 154, 810, C.stamp);

// Client
box(slide, '使用者端 Web', 'Landing / Home / Account / Loan / Card\n申請、查詢、繳費與通知設定', 370, 330, 360, 110, { accent: C.sage });
box(slide, '管理端 Web', 'Admin Dashboard / Review / Logs\n審核案件、管理客戶、檢視紀錄', 850, 330, 390, 110, { accent: C.stamp });
box(slide, 'Vue 3 Frontend', 'Vue Router + Pinia + Component Views\n以 Axios 統一呼叫後端 API', 1360, 330, 330, 110, { accent: C.sageDark });

line(slide, 730, 385, 850, 385, { color: C.border, width: 1, transparency: 35, endArrowType: 'triangle' });
line(slide, 1240, 385, 1360, 385, { color: C.border, width: 1, transparency: 35, endArrowType: 'triangle' });

// API/Security
box(slide, 'Axios API Client', 'Request Interceptor\n自動附帶 JWT Token', 370, 482, 300, 92, { accent: C.sage });
box(slide, 'RESTful API', 'Customer / Account / Loan / Card / Admin', 770, 482, 430, 92, { fill: C.sageDark, stroke: C.sageDark, accent: C.stamp, titleColor: C.paper, subColor: C.paper, subSize: 9.8 });
box(slide, 'Spring Security', 'JWT 驗證 / 權限控管 / Login Audit\n依角色與登入狀態保護 API', 1300, 482, 390, 92, { accent: C.stamp });

line(slide, 1530, 440, 1530, 482, { color: C.sage, width: 1.3, transparency: 15 });
line(slide, 670, 528, 770, 528, { color: C.sage, width: 1.3, transparency: 15 });
line(slide, 1200, 528, 1300, 528, { color: C.sage, width: 1.3, transparency: 15 });

// Backend container
rect(slide, 344, 616, 960, 178, { fill: C.paper, stroke: C.border, radius: 28 });
txt(slide, 'Spring Boot Application', 382, 634, 360, 34, {
  fontFace: fonts.serif,
  size: 17,
  bold: true,
  color: C.ink,
});
txt(slide, 'Controllers / Services / Repositories', 760, 640, 400, 26, {
  size: 11.6,
  color: C.muted,
  align: 'right',
});

const modules = [
  ['客戶與權限', 'Customer / Auth'],
  ['帳戶模組', 'Account / Transfer'],
  ['貸款模組', 'Loan / Repayment'],
  ['風控模組', 'Credit / Risk'],
  ['信用卡模組', 'Card / Bill / Txn'],
  ['通知紀錄', 'Notification / Logs'],
];
modules.forEach((m, i) => {
  const x = 382 + (i % 3) * 290;
  const y = 688 + Math.floor(i / 3) * 58;
  rect(slide, x, y, 246, 44, {
    fill: i % 2 ? C.white : C.card,
    stroke: C.border,
    radius: 14,
    strokeW: 0.5,
  });
  txt(slide, m[0], x + 16, y + 7, 118, 18, { size: 10.7, bold: true, color: C.sageDark });
  txt(slide, m[1], x + 16, y + 25, 200, 15, { size: 7.9, color: C.muted });
});

box(slide, '外部服務整合', 'Email 驗證 / 登入通知\nLine Pay 信用卡繳費流程', 1390, 632, 300, 126, {
  accent: C.stamp,
  fill: C.card,
});
line(slide, 1300, 704, 1390, 704, { color: C.stamp, width: 1.1, transparency: 20 });

// Persistence
box(slide, 'JPA / Hibernate', 'Repository 封裝資料存取\nEntity Mapping', 370, 818, 390, 76, { accent: C.sageDark, titleSize: 14 });
box(slide, 'MSSQL Database', 'Customer / Account / Loan / Risk / Card / Txn', 900, 818, 420, 76, {
  fill: C.sageDark,
  stroke: C.sageDark,
  accent: C.stamp,
  titleColor: C.paper,
  subColor: C.paper,
  subSize: 8.8,
});
box(slide, '資料狀態回寫', '審核、繳費、交易、通知狀態回到前台查詢', 1460, 818, 270, 76, { accent: C.stamp });

line(slide, 1060, 578, 1060, 616, { color: C.sage, width: 1.4 });
line(slide, 824, 794, 690, 818, { color: C.sage, width: 1.2 });
line(slide, 760, 856, 900, 856, { color: C.sage, width: 1.4 });
line(slide, 1320, 856, 1460, 856, { color: C.stamp, width: 1.2 });
line(slide, 1594, 818, 1594, 594, { color: C.stamp, width: 1, transparency: 35, endArrowType: 'triangle' });

// Bottom legend
chip(slide, '前台功能', 118, 964, 160, { fill: C.white, color: C.sageDark });
chip(slide, '後台審核', 302, 964, 160, { fill: C.white, color: C.stamp });
chip(slide, 'JWT 保護 API', 486, 964, 180, { fill: C.sageSoft, color: C.sageDark, stroke: C.sageSoft });
chip(slide, 'JPA 寫入 MSSQL', 690, 964, 200, { fill: C.stampSoft, color: C.stamp, stroke: C.stampSoft });
txt(slide, 'Architecture Overview / EEIT215 第四組', 1294, 968, 420, 30, {
  fontFace: fonts.display,
  size: 11,
  color: C.faint,
  charSpace: 3,
  align: 'right',
});

pptx.writeFile({ fileName: out }).then(() => {
  console.log(out);
}).catch((err) => {
  console.error(err);
  process.exit(1);
});
