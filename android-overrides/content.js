/* AiStudy10 Android companion — only on omp.aistudy.uz.
   No token harvesting, no direct completion API, no fabricated checkmarks.
   Video playback automation is best-effort; server decides actual progress.
*/
(function () {
  'use strict';
  if (location.hostname !== 'omp.aistudy.uz') return;
  const existing = window.__AIS_ANDROID;
  if (existing) { existing.configure(window.__AIS_ANDROID_OPTIONS || {}); return; }
  const conf = Object.assign({rate:3,autoplay:true,autoNext:true,autoStartTest:true,quizAssist:false,guessMissing:false,autoSubmit:false,autoCourse:true,replayMissing:true},window.__AIS_ANDROID_OPTIONS || {});
  const clean = x => String(x || '').normalize('NFKC').toLowerCase()
    .replace(/[’‘`ʼʻ'"“”_-]/g,'').replace(/[^\p{L}\p{N}\s]/gu,' ').replace(/\s+/g,' ').trim();
  const txt = el => String(el?.innerText || el?.textContent || '').replace(/\s+/g,' ').trim();
  const shown = el => {
    if (!el?.getClientRects?.().length) return false;
    try {const s = getComputedStyle(el);return s.display!=='none' && s.visibility!=='hidden';}catch(_){return false;}
  };
  let currentVideo=null, lastNext=0, lastQuizAction=0, lastStep='', wasOffline=false, pendingVideoEnd=null;
  const handled=new WeakSet();
  const startedTests=new WeakSet();
  const db=[{"question":"eng yaxshi tavsiflaydi","answer":"malumotlardan organishi mumkin"},{"question":"real hayotiy qollanishi","answer":"yuqoridagilarning barchasi"},{"question":"hayot siklining 2 ta bosqichi","answer":"orgatish va organish"},{"question":"expert bolgan suniy intellekt tizimi","answer":"tor si"},{"question":"3 ta yonalishni kichigidan kattasiga","answer":"chuqur organish mashinani organish"},{"question":"tyuring testi nima","answer":"inson kabi oylay olishi"},{"question":"generativ si suniy intellekt nima","answer":"yangi kontent yarata oladigan"},{"question":"llm nimani anglatadi","answer":"katta til modeli"},{"question":"chat botlar va qidiruv tizimlari ortasidagi","answer":"kontekstga etibor"},{"question":"prompt nima","answer":"chat botga kiritadigan matn"},{"question":"yaxshi promptni tuzishning","answer":"kontekst rol vazifa"},{"question":"gallyutsinatsiyalar hallucinations nima","answer":"notogri javoblar"},{"question":"prompt yozishda quyidagi yondashuvlardan","answer":"rol kontekst vazifa"},{"question":"yaratilgan matnning asosiy cheklovlaridan","answer":"xatolar bolishi mumkin"},{"question":"ohangini ozgartirish uchun nimani","answer":"ohang sozlamalari"},{"question":"hr yollash jarayonlarida foydalanishning","answer":"tegishli tajribani ajratib olishi mumkin"},{"question":"reklama grafikalarini yaratishda qaysi","answer":"yuqoridagilarning barchasi"},{"question":"maxfiy hujjatlarni baham kormaslik","answer":"maxfiylik kafolatlari yoq"},{"question":"ishga oid vazifalar uchun si dan","answer":"har doim tekshirib turing"},{"question":"tasvir yaratish uchun qaysi","answer":"DALL-E / Midjourney / Stable Diffusion"},{"question":"yaratish uchun qaysi sun'iy intellekt vositasi OpenAI tomonidan ishlab chiqilgan","answer":"DALL-E"},{"question":"yordamida tasvirlarni yaratishda qaysi omil natijani sezilarli darajada","answer":"va tavsiflovchi promtlar"},{"question":"foydalanib nimaga erishish mumkin","answer":"dasturiy ravishda"},{"question":"intellekt yordamida musiqa yarata oladi","answer":"Suno"},{"question":"oshirishga imkon beradigan asosiy funksiyasi qaysi","answer":"pscale tugmalari"},{"question":"vositalaridan qaysi biri matndan video yaratish uchun ishlatiladi","answer":"Runway ML"},{"question":"ushbu kursda si nima deb tariflangan","answer":"organadigan dasturiy taminot"},{"question":"tor si narrow ai ga misol","answer":"yuqoridagilarning barchasi"},{"question":"tyuring testining maqsadi nima","answer":"insonni ozini ham inson deb"},{"question":"uyidagilardan qaysi biri mashhur generativ","answer":"ChatGPT"},{"question":"zor prompt yaratish uchun asosiy","answer":"rol kontekst vazifa"},{"question":"si dagi gallyutsinatsiya ga misol","answer":"notogri yoki uydirma"},{"question":"lumotlarni tahlil qilish uchun qanday ishlatilishi mumkin","answer":"ganish va vizuallashtirish orqali"},{"question":"microsoft office ilovalariga qaysi","answer":"copilot"},{"question":"si va ish haqida qaysi fikr togri","answer":"samarali boladi"},{"question":"tomonidan ishlab chiqilgan SI vositasi qaysi","answer":"DALL-E"},{"question":"atndan video yaratish imkonini beruvchi","answer":"Runway ML"},{"question":"midjourney tasvir yaratishni sozlashda","answer":"tomonlar nisbati"}];

  function videoKey() {
    const video = document.querySelector('video');
    const src=video?.currentSrc || video?.src || '';
    const title=activeLessonTitle() || '';
    return 'aistudy_resume_android_v1:'+location.pathname+':'+(title || src.slice(-90));
  }
  function activeLessonTitle() {
    const els=[...document.querySelectorAll('[aria-current="page"], [aria-selected="true"], [data-state="active"], .active-module, .bg-main-primary')];
    const row=els.find(el=>/\b\d{1,2}:\d\d\b/.test(txt(el)) && txt(el).length<160);
    return row ? txt(row).replace(/\d{1,2}:\d\d/g,'').trim().slice(0,110) : '';
  }
  function setSpeed(video) {
    if(!video || !Number.isFinite(+conf.rate)) return;
    const rate=Math.max(0.5,Math.min(5,+conf.rate));
    try {if(Math.abs(video.playbackRate-rate)>.01)video.playbackRate=rate;}catch(_){}
  }
  function attachVideo(video) {
    if(handled.has(video))return;
    handled.add(video);
    video.muted = true; // Many mobile WebViews only allow autoplay when muted.
    const key=videoKey();
    function restore() {
      try {
        const saved=JSON.parse(localStorage.getItem(key)||'null');
        if(saved&&Date.now()-saved.at<7*86400000&&saved.time>2&&Number.isFinite(video.duration)&&video.duration>saved.time+4) {
          video.currentTime=saved.time;
        }
      }catch(_){}
    }
    video.addEventListener('loadedmetadata',restore,{once:true});
    if(video.readyState>=1)restore();
    video.addEventListener('ratechange',()=>setSpeed(video));
    video.addEventListener('timeupdate',()=>{
      const now=Date.now();
      if(now-(video.__lastAISSave||0)<2700 || !Number.isFinite(video.currentTime))return;
      video.__lastAISSave=now;
      try{localStorage.setItem(key,JSON.stringify({time:video.currentTime,at:now}));}catch(_){}
    });
    video.addEventListener('ended',()=>{
      try{localStorage.removeItem(key);}catch(_){}
      if(conf.autoNext) pendingVideoEnd={at:Date.now(),key};
    });
    setSpeed(video);
    if(conf.autoplay)video.play().catch(()=>{});
  }
  function watchVideos() {
    if(!location.pathname.includes('/courses/'))return;
    for(const video of document.querySelectorAll('video')) {
      if(!handled.has(video))attachVideo(video);
      setSpeed(video);
      if(conf.autoplay && video.paused && !video.ended && !wasOffline && video.readyState>=2) {
        const now=Date.now();
        if(now-(video.__lastAISPlay||0)>6500){video.__lastAISPlay=now;video.play().catch(()=>{});}
      }
    }
  }
  // Find lesson rows by their time labels, not by random buttons elsewhere on the page.
  function lessons() {
    const times=[...document.querySelectorAll('span,small,time,p,div')]
      .filter(el=>shown(el)&&/^(?:\d{1,2}:)?\d{1,2}:\d{2}$/.test(txt(el)));
    const leaves=times.filter(el=>!times.some(o=>o!==el&&el.contains(o)));
    const seen=new Set(),out=[];
    for(const leaf of leaves) {
      let node=leaf,best=null;
      for(let depth=0;node&&depth<9&&node!==document.body;depth++,node=node.parentElement){
        const text=txt(node);
        if(text.length>220 || /\b(?:test|quiz|progress|modul)\b/i.test(text))break;
        if(leaves.filter(e=>node.contains(e)).length!==1)break;
        const title=text.replace(/(?:^|\s)\d{1,2}:\d{2}(?=\s|$)/,'').trim();
        if(title.length>=7 && title!==text && shown(node))best=node;
      }
      if(best&&!seen.has(best)){
        seen.add(best);
        out.push({node:best,title:txt(best).replace(/(?:^|\s)\d{1,2}:\d{2}(?=\s|$)/,'').trim(),active:isActive(best),watched:isWatched(best)});
      }
    }
    return out;
  }
  function isActive(el) {
    for(const node of [el,el.parentElement]){
      if(!node)continue;
      if(node.getAttribute('aria-current')==='page'||node.getAttribute('aria-selected')==='true'||node.getAttribute('data-state')==='active')return true;
      if(/\b(?:active|selected|bg-main-primary|bg-blue-600|bg-blue-700)\b/.test(String(node.className)))return true;
      try{const c=getComputedStyle(node).backgroundColor.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);if(c&&(+c[3])>(+c[1])*1.5&&(+c[3])>(+c[2])*1.2&&(+c[3])>105)return true;}catch(_){}
    }
    return false;
  }
  function isWatched(el) {
    if(el.querySelector('[aria-checked="true"],input[type="checkbox"]:checked,[data-state="completed"],[data-status="watched"]'))return true;
    const icons=[...el.querySelectorAll('svg')];
    for(const svg of icons){
      const marker=[svg.getAttribute('class'),svg.getAttribute('data-lucide'),svg.getAttribute('aria-label')].join(' ');
      if(/check.circle|circle.check|checkmark|lucide-check/i.test(marker))return true;
      for(const path of svg.querySelectorAll('path,polyline')){
        const points=(path.getAttribute('points')||'').replace(/\s+/g,' ');
        const d=(path.getAttribute('d')||'').replace(/\s+/g,' ');
        if(/9[ ,]+12\s+11[ ,]+14\s+15[ ,]+10/.test(points)||/m9\s*12\s*2\s*2\s*4\s*-4/i.test(d))return true;
      }
    }
    return /^✓|✔|✅/.test(txt(el).slice(0,2));
  }
  function clickRow(row){
    if(!row||!shown(row.node))return false;
    const parent=row.node;const candidate=parent.matches('button,a,[role="button"],.cursor-pointer')?parent:
      parent.querySelector('button,a,[role="button"],.cursor-pointer')||parent;
    if(!shown(candidate)||candidate.disabled||candidate.getAttribute('aria-disabled')==='true')return false;
    try{candidate.scrollIntoView({block:'nearest'});candidate.click();return true;}catch(_){return false;}
  }
  function findControl(rx) {
    const els=[...document.querySelectorAll('button,[role="button"],a')].filter(shown).filter(el=>{
      const t=txt(el),a=el.getAttribute('aria-label')||'';
      return t.length<90&&(rx.test(t)||rx.test(a))&&!el.disabled&&el.getAttribute('aria-disabled')!=='true';
    });
    return els.length===1?els[0]:null;
  }
  function advanceLesson() {
    if(!conf.autoNext || Date.now()-lastNext<3500)return;
    const rows=lessons(),active=rows.filter(row=>row.active);
    if(active.length!==1)return; // Don't guess when active lesson is ambiguous.
    const index=rows.indexOf(active[0]);
    const nextRow=rows[index+1]||null;
    if(nextRow){if(clickRow(nextRow))lastNext=Date.now();return;}
    const test=findControl(/^(test|testga o.tish|yakuniy test|modul testi)$/i) ||
      [...document.querySelectorAll('div.cursor-pointer,li,[role="button"]')]
      .filter(shown).find(el=>/^test\s*(?:\d+)?$/i.test(txt(el).trim())&&txt(el).length<24);
    if(test){test.click();lastNext=Date.now();notify('Test bo‘limiga o‘tildi');}
    else if(conf.autoCourse){
      const course=findControl(/^(keyingi kursga o.tish|keyingi modulga o.tish|hozir boshlash|kursni boshlash)$/i);
      if(course){course.click();lastNext=Date.now();notify('Keyingi kursga o‘tilmoqda');}
    }
  }

  function quiz() {
    if(!conf.quizAssist || Date.now()-lastQuizAction<1700 || !location.pathname.includes('/courses/'))return;
    const start=findControl(/^(testni boshlash|sinovni boshlash|imtihonni boshlash|start test|start quiz)$/i);
    if(start){start.click();lastQuizAction=Date.now();return;}
    const boxes=[...document.querySelectorAll('section.px-4.py-5,div.px-4.py-5,[class*="py-5"],form')].filter(shown);
    let found=null;
    for(const box of boxes){
      const h=box.querySelector('h1.title-primary,h1,h2,div.text-lg,p.font-semibold');
      if(!h)continue;
      const title=clean(txt(h));if(title.length<8)continue;
      const opts=[...box.querySelectorAll('label,p.cursor-pointer,li,div.cursor-pointer,[role="radio"]')]
        .filter(shown).filter(el=>txt(el).length>2&&txt(el).length<320);
      const uniq=[...new Map(opts.map(o=>[clean(txt(o)),o])).values()];
      if(uniq.length<2||uniq.length>12)continue;
      found={title,opts:uniq};break;
    }
    if(!found)return;
    const selected=found.opts.some(o=>o.querySelector('input:checked,[aria-checked="true"]')||
      o.getAttribute('aria-checked')==='true'||/\b(?:selected|bg-main-primary|border-blue-600)\b/.test(String(o.className)));
    if(!selected){
      const candidates=db.filter(p=>{const q=clean(p.question);return q.length>=8&&found.title.includes(q);})
        .sort((a,b)=>clean(b.question).length-clean(a.question).length);
      const a=candidates.length?clean(candidates[0].answer):'';
      const hits=a?found.opts.filter(o=>clean(txt(o))===a||(a.length>5&&clean(txt(o)).includes(a))):[];
      if(hits.length===1){hits[0].click();lastQuizAction=Date.now();notify('Bazadan javob belgilandi');return;}
      if(conf.guessMissing && found.opts.length>0){found.opts[0].click();lastQuizAction=Date.now();notify('Taxminiy javob tanlandi');}
      return;
    }
    const progress=(document.body.innerText||'').match(/(?:savol|question)\s*:?\s*(\d+)\s*\/\s*(\d+)/i);
    const cur=progress?+progress[1]:0,all=progress?+progress[2]:0;
    const next=findControl(/^(keyingi|next)$/i);
    if(next&&cur&&cur<all){next.click();lastQuizAction=Date.now();return;}
    if(conf.autoSubmit && cur>0 && cur===all){
      const submit=findControl(/^(yuborish|yakunlash|finish|testni yakunlash)$/i);
      if(submit){submit.click();lastQuizAction=Date.now();notify('Test yuborildi');}
    }
  }

  function startTestIfPresent() {
    if(!conf.autoStartTest || !location.pathname.includes('/courses/'))return;
    const start=findControl(/^(testni boshlash|sinovni boshlash|imtihonni boshlash|start test|start quiz)$/i);
    if(!start || startedTests.has(start))return;
    try{start.scrollIntoView({block:'center'});start.click();startedTests.add(start);lastQuizAction=Date.now();notify('Testni boshlash bosildi');}catch(_){}
  }

  function confirmEndedVideo() {
    if(!pendingVideoEnd || !conf.autoNext)return;
    const rows=lessons(),active=rows.filter(row=>row.active);
    if(active.length!==1)return;
    // Wait for the site's own watched marker; never infer completion from the
    // media ended event or write progress ourselves.
    if(active[0].watched){
      pendingVideoEnd=null;
      advanceLesson();
      return;
    }
    if(Date.now()-pendingVideoEnd.at>30000){
      pendingVideoEnd=null;
      notify('Video tugadi, lekin sayt ko‘rilganini hali tasdiqlamadi');
    }
  }

  window.addEventListener('offline',()=>{wasOffline=true;});
  window.addEventListener('online',()=>{
    if(!wasOffline)return;
    wasOffline=false;
    // Avoid losing quiz choices after connectivity recovery.
    if(!/\b(?:savol|question)\s*\d+\s*\/\s*\d+/i.test(document.body.innerText||''))location.reload();
  });
  window.__AIS_ANDROID={configure(o){Object.assign(conf,o||{});for(const v of document.querySelectorAll('video'))setSpeed(v);}};
  setInterval(()=>{
    // Background sessions remain enabled; Android may still suspend them under memory pressure.
    watchVideos();confirmEndedVideo();startTestIfPresent();quiz();
  },1300);
  watchVideos();

  let nativeStatusTime=0;
  function notify(text){
    const now=Date.now();if(now-nativeStatusTime<1800)return;
    nativeStatusTime=now;
    try{browser.runtime.sendNativeMessage('aistudy',{type:'status',text}).catch(()=>{});}catch(_){}
  }
  function syncNative(){
    try{browser.runtime.sendNativeMessage('aistudy',{type:'config'})
      .then(o=>{if(o&&typeof o==='object')window.__AIS_ANDROID.configure(o);})
      .catch(()=>{});
    }catch(_){}
  }
  setInterval(syncNative,5000);
  syncNative();
  notify('Dars sahifasi ochiq');
})();
