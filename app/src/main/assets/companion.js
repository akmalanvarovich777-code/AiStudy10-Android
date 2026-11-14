/* AiStudy10 Android companion — only on omp.aistudy.uz.
   No token harvesting, no direct completion API, no fabricated checkmarks.
   Video playback automation is best-effort; server decides actual progress.
*/
(function () {
  'use strict';
  if (location.hostname !== 'omp.aistudy.uz') return;
  const existing = window.__AIS_ANDROID;
  if (existing) { existing.configure(window.__AIS_ANDROID_OPTIONS || {}); return; }
  const conf = Object.assign({rate:3,autoplay:true,autoNext:true,quizAssist:false,guessMissing:false,autoSubmit:false},window.__AIS_ANDROID_OPTIONS || {});
  const clean = x => String(x || '').normalize('NFKC').toLowerCase()
    .replace(/[’‘`ʼʻ'"“”_-]/g,'').replace(/[^\p{L}\p{N}\s]/gu,' ').replace(/\s+/g,' ').trim();
  const txt = el => String(el?.innerText || el?.textContent || '').replace(/\s+/g,' ').trim();
  const shown = el => {
    if (!el?.getClientRects?.().length) return false;
    try {const s = getComputedStyle(el);return s.display!=='none' && s.visibility!=='hidden';}catch(_){return false;}
  };
  let currentVideo=null, lastNext=0, lastQuizAction=0, lastStep='', wasOffline=false;
  const handled=new WeakSet();
  const db=Array.isArray(window.__AIS_ANDROID_ANSWERS)?window.__AIS_ANDROID_ANSWERS:[];

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
    const rate=Math.max(0.5,Math.min(3,+conf.rate));
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
      if(conf.autoNext) setTimeout(()=>advanceLesson(),1500);
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
    let nextRow=null;
    for(let i=index+1;i<rows.length;i++)if(!rows[i].watched){nextRow=rows[i];break;}
    if(nextRow){if(clickRow(nextRow))lastNext=Date.now();return;}
    const test=findControl(/^(test|testga o.tish|yakuniy test|modul testi)$/i);
    if(test){test.click();lastNext=Date.now();}
  }

  function quiz() {
    if(!conf.quizAssist || Date.now()-lastQuizAction<1700 || !location.pathname.includes('/courses/'))return;
    const start=findControl(/^(testni boshlash|sinovni boshlash|start test)$/i);
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
      if(!candidates.length)return;
      const a=clean(candidates[0].answer);
      const hits=found.opts.filter(o=>clean(txt(o))===a||(a.length>5&&clean(txt(o)).includes(a)));
      if(hits.length!==1)return;
      hits[0].click();lastQuizAction=Date.now();return;
    }
    const progress=(document.body.innerText||'').match(/(?:savol|question)\s*:?\s*(\d+)\s*\/\s*(\d+)/i);
    const cur=progress?+progress[1]:0,all=progress?+progress[2]:0;
    const next=findControl(/^(keyingi|next)$/i);
    if(next&&cur&&cur<all){next.click();lastQuizAction=Date.now();}
    // No automatic final submission on Android: user reviews answer choices before submission.
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
    if(document.hidden)return; // Android may suspend background pages; never claim 10 simultaneous streams.
    watchVideos();quiz();
  },1300);
  watchVideos();
})();
