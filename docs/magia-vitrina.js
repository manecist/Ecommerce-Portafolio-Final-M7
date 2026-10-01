/* ==========================================================================
   magia-vitrina.js · la vitrina se presenta como el portafolio de Ari:
   - un portal mágico de entrada para elegir cómo recorrer la tienda
   - cielo con estrellas y destellos que siguen al cursor
   - tarjetas que se inclinan, se revelan al bajar y vuelan al caldero
   - un hada guía con un tour paso a paso
   © 2026 María Inés Cisterna Escobar. Todos los derechos reservados.
   ========================================================================== */
(function () {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s), $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const quieto = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const tocar = matchMedia('(hover: none)').matches;
  const COLORES = ['#ff8fc0', '#ffd9ea', '#c9a6ff', '#f3d48a', '#ffffff'];

  /* ------------------------------------------------------------ partículas */
  const cv = document.createElement('canvas'); cv.className = 'mv-cielo'; cv.setAttribute('aria-hidden', 'true');
  document.body.append(cv);
  const c = cv.getContext('2d'); let parts = [];
  const medir = () => { cv.width = innerWidth * devicePixelRatio; cv.height = innerHeight * devicePixelRatio; c.setTransform(devicePixelRatio, 0, 0, devicePixelRatio, 0, 0); };
  medir(); addEventListener('resize', medir);
  function chispas(x, y, n = 18, vel = 3) {
    if (quieto) return;
    for (let i = 0; i < n; i++) {
      const a = Math.random() * Math.PI * 2, v = Math.random() * vel + .6;
      parts.push({ x, y, vx: Math.cos(a) * v, vy: Math.sin(a) * v - 1, r: Math.random() * 2.6 + 1, vida: 1, col: COLORES[i % COLORES.length], estrella: Math.random() < .4 });
    }
  }
  function estrella(x, y, r) { c.beginPath(); for (let k = 0; k < 8; k++) { const rr = k % 2 ? r * .4 : r * 1.6, a = k * Math.PI / 4; c.lineTo(x + Math.cos(a) * rr, y + Math.sin(a) * rr); } c.closePath(); c.fill(); }
  (function cuadro() {
    c.clearRect(0, 0, innerWidth, innerHeight);
    parts = parts.filter(p => p.vida > 0);
    for (const p of parts) {
      p.x += p.vx; p.y += p.vy; p.vy += .05; p.vx *= .98; p.vida -= .018;
      c.globalAlpha = Math.max(0, p.vida); c.fillStyle = p.col;
      if (p.estrella) estrella(p.x, p.y, p.r); else { c.beginPath(); c.arc(p.x, p.y, p.r, 0, 7); c.fill(); }
    }
    c.globalAlpha = 1;
    requestAnimationFrame(cuadro);
  })();
  let ultimo = 0;
  if (!tocar) addEventListener('pointermove', e => { const t = performance.now(); if (t - ultimo > 40) { ultimo = t; chispas(e.clientX, e.clientY, 2, 1.2); } }, { passive: true });
  addEventListener('pointerdown', e => { if (!e.target.closest('input,textarea,select')) chispas(e.clientX, e.clientY, 14, 3.2); }, { passive: true });

  /* ------------------------------------------------------------ portal de entrada */
  function portal() {
    let visto = false; try { visto = sessionStorage.getItem('mv-portal') === '1'; } catch (e) { /* sin almacenamiento */ }
    if (visto) return;
    const el = document.createElement('div'); el.className = 'mv-portal'; el.setAttribute('role', 'dialog'); el.setAttribute('aria-modal', 'true'); el.setAttribute('aria-label', 'Bienvenida a Magical Alliance');
    el.innerHTML = `<canvas aria-hidden="true"></canvas><a class="mv-volver" href="https://manecist.github.io/">← Volver al portafolio de Ari</a>
      <div class="mv-circulo" aria-hidden="true"></div>
      <div class="mv-portal-centro">
        <img class="mv-logo" src="img/logo-texto.webp" alt="Magical Alliance">
        <h1>¿Recuerdas la magia de la transformación?</h1>
        <p>Esta es la vitrina de mi eCommerce Full Stack Java. Todo funciona aquí mismo, en tu navegador. ¿Cómo quieres entrar?</p>
        <div class="mv-opciones">
          <button type="button" data-entrar="CLIENT">🛍️ Como clienta, con tour</button>
          <button type="button" data-entrar="ADMIN">🛡️ Como administradora</button>
          <button type="button" class="alt" data-entrar="INVITADO">✦ Solo mirar</button>
        </div>
      </div>`;
    document.body.append(el); document.body.style.overflow = 'hidden';
    // estrellas del portal
    const pc = el.querySelector('canvas'), x = pc.getContext('2d');
    const est = Array.from({ length: 140 }, () => ({ x: Math.random(), y: Math.random(), r: Math.random() * 1.6 + .3, f: Math.random() * 6 }));
    let vivo = true;
    (function cielo(t) {
      if (!vivo) return;
      pc.width = innerWidth; pc.height = innerHeight;
      for (const s of est) { x.globalAlpha = .3 + .7 * Math.abs(Math.sin(s.f + t / 900)); x.fillStyle = '#fff'; x.beginPath(); x.arc(s.x * pc.width, s.y * pc.height, s.r, 0, 7); x.fill(); }
      requestAnimationFrame(cielo);
    })(0);
    setTimeout(() => { const r = el.querySelector('.mv-logo').getBoundingClientRect(); chispas(r.left + r.width / 2, r.top + r.height / 2, 60, 5); }, quieto ? 0 : 1300);
    el.querySelector('.mv-opciones button')?.focus({ preventScroll: true });
    el.addEventListener('click', e => {
      const b = e.target.closest('[data-entrar]'); if (!b) return;
      const r = b.getBoundingClientRect(); chispas(r.left + r.width / 2, r.top + r.height / 2, 50, 5);
      try { sessionStorage.setItem('mv-portal', '1'); } catch (er) { /* sin almacenamiento */ }
      const rol = b.dataset.entrar;
      const btnRol = $(`[data-action="rol"][data-rol="${rol}"]`);
      if (btnRol && btnRol.getAttribute('aria-pressed') !== 'true') btnRol.click();
      el.classList.add('cierra'); document.body.style.overflow = '';
      setTimeout(() => { vivo = false; el.remove(); if (rol !== 'INVITADO') tour(rol); }, quieto ? 0 : 900);
    });
  }

  /* ------------------------------------------------------------ hada guía */
  const hada = document.createElement('div'); hada.className = 'mv-hada';
  hada.innerHTML = '<div class="mv-globo" role="status" hidden></div><img src="img/hada.webp" alt="Hada guía: toca para un tour mágico" title="¿Un tour mágico?">';
  document.body.append(hada);
  const globo = hada.querySelector('.mv-globo');
  let foco = null;
  const quitarFoco = () => { foco?.classList.remove('mv-foco'); foco = null; };
  const PASOS = {
    CLIENT: [
      ['#/inicio', '.navbar-mistica', '¡Hola! Soy el hada de <b>Magical Alliance</b>. Arriba está el menú: Productos, Mis pedidos y Tips.'],
      ['#/catalogo', '#app .card-magica', 'Este es el <b>catálogo</b>. Filtra por categoría, busca y pulsa <b>Añadir al caldero</b>: verás volar el tesoro.'],
      ['#/catalogo', '.btn-carrito-magic:not(.d-none), .btn-carrito-magic', 'Aquí está tu <b>caldero</b> (el carrito). El numerito cuenta tus tesoros.'],
      ['#/caldero', '#app', 'En el caldero cambias cantidades y aplicas el cupón <b>MAGIC20</b> antes del checkout.'],
      ['#/mis-pedidos', '#app', 'Después de comprar, tus pedidos aparecen aquí con su estado. ¡Igual que en la app Spring Boot!']
    ],
    ADMIN: [
      ['#/admin/resumen', '#adminBar', 'Como administradora tienes la barra de <b>Gestión mágica</b>: resumen, productos, pedidos, descuentos y cupones.'],
      ['#/admin/productos', '#app', 'Crea, edita o elimina productos y ajusta el <b>stock</b> con un toque.'],
      ['#/admin/pedidos', '#app', 'Cambia el estado de los pedidos: pagado, enviado, entregado… y el stock se mantiene coherente.'],
      ['#/admin/cupones', '#app', 'Y aquí se crean los <b>cupones</b> que usan las clientas. ¡Eso es todo! Toca el hada cuando quieras repetir.']
    ]
  };
  function tour(rol) {
    const pasos = PASOS[rol] || PASOS.CLIENT; let i = 0;
    const mostrar = () => {
      quitarFoco();
      const [ruta, sel, texto] = pasos[i];
      if (location.hash !== ruta) location.hash = ruta;
      setTimeout(() => {
        const objetivo = $$(sel).find(n => n.offsetParent !== null);
        if (objetivo) { foco = objetivo; objetivo.classList.add('mv-foco'); objetivo.scrollIntoView({ behavior: quieto ? 'auto' : 'smooth', block: 'center' }); const r = objetivo.getBoundingClientRect(); chispas(r.left + r.width / 2, r.top + 20, 24, 3.5); }
        globo.hidden = false;
        globo.innerHTML = `<div>${texto}</div><div class="mv-btns"><button type="button" class="alt" data-tour="fin">Cerrar</button><button type="button" data-tour="sig">${i < pasos.length - 1 ? 'Siguiente ✦' : '¡Listo!'}</button></div>`;
        globo.querySelector('[data-tour="sig"]').focus({ preventScroll: true });
      }, 450);
    };
    globo.onclick = e => {
      const b = e.target.closest('[data-tour]'); if (!b) return;
      if (b.dataset.tour === 'sig' && i < pasos.length - 1) { i++; mostrar(); }
      else { quitarFoco(); globo.hidden = true; }
    };
    mostrar();
  }
  hada.querySelector('img').addEventListener('click', () => {
    const rol = $('[data-action="rol"][aria-pressed="true"]')?.dataset.rol;
    if (!globo.hidden) { quitarFoco(); globo.hidden = true; return; }
    if (rol === 'INVITADO') {
      globo.hidden = false;
      globo.innerHTML = '<div>¿Un tour mágico? Elige cómo recorrer la tienda.</div><div class="mv-btns"><button type="button" class="alt" data-elegir="ADMIN">Administradora</button><button type="button" data-elegir="CLIENT">Clienta</button></div>';
      globo.onclick = e => { const b = e.target.closest('[data-elegir]'); if (!b) return; $(`[data-action="rol"][data-rol="${b.dataset.elegir}"]`)?.click(); setTimeout(() => tour(b.dataset.elegir), 300); };
    } else tour(rol);
  });
  addEventListener('keydown', e => { if (e.key === 'Escape' && !globo.hidden) { quitarFoco(); globo.hidden = true; } });

  /* ------------------------------------------------------------ tarjetas que se inclinan y vuelan al caldero */
  if (!tocar && !quieto) {
    document.addEventListener('pointermove', e => {
      const card = e.target.closest?.('.card-magica'); if (!card) return;
      const r = card.getBoundingClientRect(), dx = (e.clientX - r.left) / r.width - .5, dy = (e.clientY - r.top) / r.height - .5;
      card.style.transform = `perspective(700px) rotateY(${dx * 10}deg) rotateX(${-dy * 10}deg) translateY(-4px)`;
    }, { passive: true });
    document.addEventListener('pointerout', e => { const card = e.target.closest?.('.card-magica'); if (card && !card.contains(e.relatedTarget)) card.style.transform = ''; });
  }
  document.addEventListener('click', e => {
    const b = e.target.closest('[data-action="agregar"]'); if (!b || b.disabled) return;
    const card = b.closest('.card-magica, .modal-content');
    const img = card?.querySelector('img');
    const dest = $$('.btn-carrito-magic').find(n => n.offsetParent !== null);
    const r = b.getBoundingClientRect(); chispas(r.left + r.width / 2, r.top, 26, 4);
    if (!img || !dest || quieto) return;
    const ri = img.getBoundingClientRect(), rd = dest.getBoundingClientRect();
    const v = document.createElement('img'); v.src = img.src; v.className = 'mv-vuela'; v.alt = '';
    v.style.left = ri.left + ri.width / 2 - 27 + 'px'; v.style.top = ri.top + ri.height / 2 - 27 + 'px';
    document.body.append(v);
    requestAnimationFrame(() => { v.style.transform = `translate(${rd.left + rd.width / 2 - (ri.left + ri.width / 2)}px,${rd.top + rd.height / 2 - (ri.top + ri.height / 2)}px) scale(.3) rotate(360deg)`; v.style.opacity = '.4'; });
    setTimeout(() => { v.remove(); dest.classList.remove('salta'); void dest.offsetWidth; dest.classList.add('salta'); chispas(rd.left + rd.width / 2, rd.top + rd.height / 2, 30, 3.5); }, 900);
  }, true);

  /* ------------------------------------------------------------ revelar al bajar */
  const io = 'IntersectionObserver' in window ? new IntersectionObserver(es => es.forEach(en => { if (en.isIntersecting) { en.target.classList.add('visto'); io.unobserve(en.target); } }), { threshold: .12 }) : null;
  function revelar() {
    if (!io || quieto) return;
    $$('#app .card-magica, #app section, #app .tip-card, #app .panel').forEach(n => { if (!n.classList.contains('mv-revela')) { n.classList.add('mv-revela'); io.observe(n); } });
  }
  new MutationObserver(revelar).observe($('#app'), { childList: true });
  revelar();

  portal();
})();
