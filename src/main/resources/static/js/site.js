(() => {
  // Keep reading position on refresh, Back/Forward, and forms that return to the
  // same page (e.g. bidding, rating, notifications). New pages still start at top.
  // A real #anchor always takes priority over restoration.
  try {
    const key = 'cholobodlai:scroll:' + location.pathname;
    const submitKey = 'cholobodlai:pending-post-origin';
    const pendingOrigin = sessionStorage.getItem(submitKey);
    const navEntry = performance.getEntriesByType('navigation')[0];
    const navType = navEntry ? navEntry.type : 'navigate';
    const shouldRestore = !location.hash && (
      navType === 'reload' || navType === 'back_forward' || pendingOrigin === location.pathname
    );
    const rememberedY = Number(sessionStorage.getItem(key));
    // Clear a POST marker on the next page, including redirects to another route.
    if (pendingOrigin) sessionStorage.removeItem(submitKey);
    let readyToSave = !shouldRestore;

    const saveScroll = () => {
      if (readyToSave) sessionStorage.setItem(key, String(Math.max(0, window.scrollY)));
    };
    if (shouldRestore && Number.isFinite(rememberedY) && rememberedY >= 0) {
      if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
      window.addEventListener('load', () => {
        requestAnimationFrame(() => requestAnimationFrame(() => {
          window.scrollTo({top: rememberedY, left: 0, behavior: 'instant'});
          readyToSave = true;
        }));
      }, {once: true});
    } else {
      readyToSave = true;
      if ('scrollRestoration' in history) history.scrollRestoration = 'auto';
    }
    window.addEventListener('scroll', saveScroll, {passive: true});
    window.addEventListener('pagehide', saveScroll);
    // GET forms (filters/search/sort) also navigate even though they don't POST.
    // Save the position before same-page query changes or a form redirect.
    document.querySelectorAll('form').forEach(form => {
      form.addEventListener('submit', () => {
        if (!form.checkValidity() || form.dataset.scrollTop === 'true') return;
        const method = (form.getAttribute('method') || 'get').toLowerCase();
        const destination = new URL(form.getAttribute('action') || location.href, location.href);
        if (method === 'get' && destination.pathname !== location.pathname) return;
        saveScroll();
        sessionStorage.setItem(submitKey, location.pathname);
      });
    });
    // A search reset or filter link may change only ?query= on this same page.
    document.querySelectorAll('a[href]').forEach(link => {
      link.addEventListener('click', () => {
        const destination = new URL(link.href, location.href);
        if (destination.origin === location.origin && destination.pathname === location.pathname
            && destination.search !== location.search && !destination.hash) {
          saveScroll();
          sessionStorage.setItem(submitKey, location.pathname);
        }
      });
    });
  } catch (_) {
    // Storage may be blocked by browser privacy settings; normal navigation still works.
  }

  // Keep the form's two-day minimum up to date if the tab stays open for hours.
  document.querySelectorAll('input[data-minimum-pickup]').forEach(input => {
    const refreshMin = () => {
      const future = new Date(Date.now() + (48 * 60 * 60 * 1000) + (60 * 1000));
      const pad = v => String(v).padStart(2, '0');
      const localMin = `${future.getFullYear()}-${pad(future.getMonth() + 1)}-${pad(future.getDate())}T${pad(future.getHours())}:${pad(future.getMinutes())}`;
      if (!input.min || localMin > input.min) input.min = localMin;
      input.setCustomValidity(input.value && input.value < input.min
        ? 'Pickup must be at least 48 hours (2 days) from now.' : '');
    };
    input.addEventListener('focus', refreshMin);
    input.addEventListener('change', refreshMin);
    input.form?.addEventListener('submit', refreshMin);
    refreshMin();
  });

  const button = document.querySelector('.mobile-toggle');
  const nav = document.querySelector('#main-navigation');
  if (button && nav) {
    button.addEventListener('click', () => {
      const isOpen = nav.classList.toggle('menu-open');
      button.setAttribute('aria-expanded', String(isOpen));
    });
    nav.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
      nav.classList.remove('menu-open');
      button.setAttribute('aria-expanded', 'false');
    }));
  }
  // Responsive operations rail: share the single menu control with header navigation.
  const rail = document.querySelector('.dispatch-rail');
  const backdrop = document.querySelector('.rail-backdrop');
  const closeRail = () => {
    rail?.classList.remove('is-open');
    nav?.classList.remove('menu-open');
    backdrop?.classList.remove('is-open');
    button?.setAttribute('aria-expanded', 'false');
  };
  if (button && rail) {
    button.addEventListener('click', () => {
      const opened = rail.classList.toggle('is-open');
      backdrop?.classList.toggle('is-open', opened);
      button.setAttribute('aria-expanded', String(opened));
    });
    rail.querySelector('.rail-close')?.addEventListener('click', closeRail);
    backdrop?.addEventListener('click', closeRail);
    rail.querySelectorAll('a[href]').forEach(link => {
      link.addEventListener('click', closeRail);
      try {
        const url = new URL(link.href, location.href);
        if (url.pathname === location.pathname) link.setAttribute('aria-current', 'page');
      } catch (_) {}
    });
    window.addEventListener('keydown', event => { if (event.key === 'Escape') closeRail(); });
  }
  // Recognize current parent section so direct trip pages still highlight workspace.
  const section = location.pathname.startsWith('/trips') ? 'trip' : '';
  if (rail && section) {
    const target = rail.querySelector('a[href="/shipper"]') || rail.querySelector('a[href="/owner"]');
    target?.classList.add('is-active');
  }

  const form = document.querySelector('[data-register-form]');
  if (form) {
    const select = form.querySelector('#role');
    const fields = form.querySelector('#ownerFields');
    const reg = form.querySelector('#truckRegistrationNo');
    const type = form.querySelector('#truckTypeId');
    const selected = new URLSearchParams(location.search).get('role');
    if (selected === 'OWNER' || selected === 'SHIPPER') select.value = selected;
    const redraw = () => {
      const isOwner = select.value === 'OWNER';
      fields.hidden = !isOwner; reg.required = isOwner; type.required = isOwner;
      fields.querySelectorAll('input,select').forEach(el => { if (el !== reg && el !== type) el.disabled = !isOwner; });
    };
    select.addEventListener('change', redraw); redraw();
  }
})();
