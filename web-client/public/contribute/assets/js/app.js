(function(){
  var STORAGE_KEY = 'argentum-guide-completed';
  var sections = Array.from(document.querySelectorAll('details.section[id^="part-"]'));
  var stepRows = Array.from(document.querySelectorAll('.step-row'));
  var totalSteps = stepRows.length;
  var totalSections = sections.length;

  function getCompleted(){ try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'); } catch(e){ return []; } }
  function setCompleted(arr){ try { localStorage.setItem(STORAGE_KEY, JSON.stringify(arr)); } catch(e){} }
  function applyCompletedState(){
    var done = getCompleted();
    sections.forEach(function(sec){
      var isDone = done.indexOf(sec.id) !== -1;
      sec.classList.toggle('completed', isDone);
      var btn = sec.querySelector('.complete-toggle');
      if(btn){ btn.setAttribute('aria-pressed', isDone); btn.textContent = isDone ? '\\u2713 Completed \\u2014 click to unmark' : 'Mark complete'; }
    });
  }
  document.addEventListener('click', function(e){
    var btn = e.target.closest('.complete-toggle');
    if(!btn) return;
    e.preventDefault();
    var sec = btn.closest('details.section');
    var done = getCompleted();
    var idx = done.indexOf(sec.id);
    if(idx === -1) done.push(sec.id); else done.splice(idx,1);
    setCompleted(done);
    applyCompletedState();
  });
  applyCompletedState();

  var stickyNav = document.getElementById('sticky-nav');
  var stickyIcons = document.getElementById('sticky-icons');
  var stickyLabel = document.getElementById('sticky-label');
  var stickyTitle = document.getElementById('sticky-title');
  var stickyStep = document.getElementById('sticky-step');
  var progressFill = document.getElementById('progress-fill');
  var btnPrev = document.getElementById('sticky-prev');
  var btnNext = document.getElementById('sticky-next');
  var currentIndex = -1;

  function updateStickyContent(idx){
    if(idx < 0 || idx >= sections.length) return;
    var sec = sections[idx];
    var num = sec.id.replace('part-','');
    var titleEl = sec.querySelector('.part-title');
    var row = sec.querySelector('.icons-row');
    stickyIcons.innerHTML = row ? row.innerHTML : '';
    stickyLabel.textContent = 'PART ' + num + ' / ' + totalSections;
    stickyTitle.textContent = titleEl ? titleEl.textContent : '';
    currentIndex = idx;
    btnPrev.disabled = idx <= 0;
    btnNext.disabled = idx >= sections.length - 1;
  }

  function goToSection(idx){
    var sec = sections[idx];
    if(!sec) return;
    sec.open = true;
    sec.scrollIntoView({behavior:'smooth', block:'start'});
  }
  btnPrev.addEventListener('click', function(){ if(currentIndex > 0) goToSection(currentIndex - 1); });
  btnNext.addEventListener('click', function(){ if(currentIndex < sections.length - 1) goToSection(currentIndex + 1); });

  /* Sticky-nav visibility is derived from geometry every scroll frame, not latched by an
     IntersectionObserver on the TOC. An observer only fires when the sentinel *crosses* a
     viewport edge, so jumping straight from deep in the page back to the top (Home key,
     in-page anchor, scroll restored on reload) left the sentinel out of view both before
     and after — no callback, the bar stayed visible, and it covered the masthead. */
  var tocSentinel = document.getElementById('toc-section');
  var masthead = document.querySelector('.masthead');
  function updateStickyVisibility(){
    var pastSentinel = tocSentinel
      ? tocSentinel.getBoundingClientRect().bottom < 0
      : window.scrollY > 200;
    var mastheadCleared = masthead ? masthead.getBoundingClientRect().bottom <= 0 : true;
    stickyNav.classList.toggle('visible', pastSentinel && mastheadCleared);
  }

  if('IntersectionObserver' in window){
    var sectionObserver = new IntersectionObserver(function(entries){
      entries.forEach(function(entry){
        if(entry.isIntersecting){
          updateStickyContent(sections.indexOf(entry.target));
        }
      });
    }, { rootMargin: '-15% 0px -75% 0px', threshold: 0 });
    sections.forEach(function(sec){ sectionObserver.observe(sec); });
  }

  var ticking = false;
  function onScroll(){
    if(!ticking){
      window.requestAnimationFrame(function(){ updateStickyVisibility(); updateProgress(); updateStepCounter(); ticking = false; });
      ticking = true;
    }
  }
  function updateProgress(){
    var doc = document.documentElement;
    var scrollTop = window.scrollY || doc.scrollTop;
    var height = doc.scrollHeight - doc.clientHeight;
    var pct = height > 0 ? (scrollTop / height) * 100 : 0;
    progressFill.style.width = pct + '%';
  }
  function updateStepCounter(){
    var current = null;
    for(var idx=0; idx<stepRows.length; idx++){
      var rect = stepRows[idx].getBoundingClientRect();
      if(rect.top <= 160){ current = idx; } else { break; }
    }
    if(current === null){ stickyStep.textContent = ''; return; }
    var badge = stepRows[current].querySelector('.step-badge');
    var n = badge ? parseInt(badge.textContent, 10) : null;
    stickyStep.textContent = n ? ('STEP ' + n + ' / ' + totalSteps) : '';
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  window.addEventListener('resize', updateStickyVisibility, { passive: true });
  /* opening/collapsing a section reflows the page without scrolling it; `toggle` doesn't bubble */
  document.addEventListener('toggle', updateStickyVisibility, true);
  updateStickyVisibility();
  updateProgress();

  // ---- search index ----
  var searchIndex = [];
  sections.forEach(function(sec){
    var titleEl = sec.querySelector('.part-title');
    var title = titleEl ? titleEl.textContent : '';
    searchIndex.push({ type:'Section', text: title, target: sec.id });
    sec.querySelectorAll('.step-row').forEach(function(row){
      var badge = row.querySelector('.step-badge');
      var txt = row.querySelector('.step-text');
      var n = badge ? badge.textContent.replace(/^0+/,'') : '';
      searchIndex.push({ type:'Step ' + n, text: txt ? txt.textContent : '', target: row.id });
    });
    sec.querySelectorAll('li').forEach(function(li){
      if(li.querySelector('strong')){
        var label = (sec.id === 'part-1') ? 'Glossary' : 'Reference';
        searchIndex.push({ type:label, text: li.textContent, target: sec.id });
      }
    });
    sec.querySelectorAll('code').forEach(function(c){
      searchIndex.push({ type:'Command', text: c.textContent, target: sec.id });
    });
  });

  function escapeHtml(s){ return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

  function wireSearch(input, resultsEl){
    function run(){
      var q = input.value.trim().toLowerCase();
      resultsEl.innerHTML = '';
      if(q.length < 2){ resultsEl.classList.remove('open'); return; }
      var matches = searchIndex.filter(function(item){ return item.text.toLowerCase().indexOf(q) !== -1; }).slice(0, 12);
      if(matches.length === 0){
        resultsEl.innerHTML = '<div class="search-empty">No results for &ldquo;' + escapeHtml(input.value) + '&rdquo;</div>';
        resultsEl.classList.add('open');
        return;
      }
      matches.forEach(function(m){
        var a = document.createElement('a');
        a.href = '#' + m.target;
        a.className = 'search-result';
        a.innerHTML = '<span class="sr-type">' + m.type + '</span><span class="sr-text">' + escapeHtml(m.text).slice(0,110) + '</span>';
        a.addEventListener('click', function(e){
          e.preventDefault();
          var target = document.getElementById(m.target);
          if(target){
            var detailsEl = target.closest('details');
            if(detailsEl) detailsEl.open = true;
            target.scrollIntoView({behavior:'smooth', block:'start'});
            target.classList.add('flash');
            setTimeout(function(){ target.classList.remove('flash'); }, 1600);
          }
          resultsEl.classList.remove('open');
          input.value = '';
        });
        resultsEl.appendChild(a);
      });
      resultsEl.classList.add('open');
    }
    input.addEventListener('input', run);
    input.addEventListener('keydown', function(e){ if(e.key === 'Escape'){ resultsEl.classList.remove('open'); input.blur(); } });
  }

  document.querySelectorAll('.search-wrap').forEach(function(wrap){
    var input = wrap.querySelector('.search-input');
    var resultsEl = wrap.querySelector('.search-results');
    if(input && resultsEl) wireSearch(input, resultsEl);
  });
  document.addEventListener('click', function(e){
    if(!e.target.closest('.search-wrap')){
      document.querySelectorAll('.search-results').forEach(function(r){ r.classList.remove('open'); });
    }
  });

  var mainSearchInput = document.querySelector('.search-input');
  document.addEventListener('keydown', function(e){
    if(e.key === '/' && document.activeElement.tagName !== 'INPUT'){
      e.preventDefault();
      if(mainSearchInput){ mainSearchInput.scrollIntoView({behavior:'smooth', block:'center'}); mainSearchInput.focus(); }
    }
  });
  var stickySearchBtn = document.getElementById('sticky-search-btn');
  if(stickySearchBtn){
    stickySearchBtn.addEventListener('click', function(){
      if(mainSearchInput){ mainSearchInput.scrollIntoView({behavior:'smooth', block:'center'}); mainSearchInput.focus(); }
    });
  }
})();
