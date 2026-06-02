class TesseraLogo extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' });
  }

  connectedCallback() {
    const template = document.createElement('template');
    template.innerHTML = `
      <style>
        :host {
          display: block;
          width: 100%;
          max-width: 700px; /* component sizing */
          margin: 0 auto;
        }
        svg {
          width: 100%;
          height: auto;
        }
        /* Core Setup & Performance Optimization */
        .animated-element { backface-visibility: hidden; will-change: transform, opacity; }
        .piece-tl { transform: translate(-350px, -350px) rotate(-60deg); opacity: 0; }
        .piece-tr { transform: translate(350px, -350px) rotate(60deg); opacity: 0; }
        .piece-bl { transform: translate(-350px, 350px) rotate(-60deg); opacity: 0; }
        .piece-br { transform: translate(350px, 350px) rotate(60deg); opacity: 0; }
        .tri-left { transform: translate(-180px, 20px); opacity: 0; }
        .tri-right { transform: translate(180px, 20px); opacity: 0; }
        .text-title { transform: translateY(35px); opacity: 0; }
        .text-sub { transform: translateY(35px); opacity: 0; }
        .play-animation .piece-tl { animation: assembleTL 1.4s cubic-bezier(0.16,1,0.3,1) forwards; }
        .play-animation .piece-tr { animation: assembleTR 1.4s cubic-bezier(0.16,1,0.3,1) forwards; }
        .play-animation .piece-bl { animation: assembleBL 1.4s cubic-bezier(0.16,1,0.3,1) forwards; }
        .play-animation .piece-br { animation: assembleBR 1.4s cubic-bezier(0.16,1,0.3,1) forwards; }
        .play-animation .tri-left { animation: slideTriLeft 1.2s cubic-bezier(0.16,1,0.3,1) 0.2s forwards; }
        .play-animation .tri-right { animation: slideTriRight 1.2s cubic-bezier(0.16,1,0.3,1) 0.2s forwards; }
        .play-animation .text-title { animation: textFadeUp 1.2s cubic-bezier(0.16,1,0.3,1) 0.5s forwards; }
        .play-animation .text-sub { animation: textFadeUp 1.2s cubic-bezier(0.16,1,0.3,1) 0.75s forwards; }
        @keyframes assembleTL { 100% { transform: translate(0,0) rotate(0deg); opacity:1; } }
        @keyframes assembleTR { 100% { transform: translate(0,0) rotate(0deg); opacity:1; } }
        @keyframes assembleBL { 100% { transform: translate(0,0) rotate(0deg); opacity:1; } }
        @keyframes assembleBR { 100% { transform: translate(0,0) rotate(0deg); opacity:1; } }
        @keyframes slideTriLeft { 100% { transform: translate(0,0); opacity:1; } }
        @keyframes slideTriRight { 100% { transform: translate(0,0); opacity:1; } }
        @keyframes textFadeUp { 100% { transform: translateY(0); opacity:1; } }
      </style>
      <svg id="tessera-logo" xmlns="http://www.w3.org/2000/svg" viewBox="190 30 620 520" width="100%" height="100%">
        <defs>
          <linearGradient id="topBlueGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
            <stop offset="49.8%" stop-color="#3A8DB5" />
            <stop offset="50.2%" stop-color="#64ACD0" />
          </linearGradient>
          <linearGradient id="bottomDarkGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
            <stop offset="49.8%" stop-color="#0E505E" />
            <stop offset="50.2%" stop-color="#176E7D" />
          </linearGradient>
          <filter id="pieceShadow" x="-30%" y="-30%" width="160%" height="160%">
            <feDropShadow dx="3" dy="6" stdDeviation="5" flood-color="#002233" flood-opacity="0.18" />
          </filter>
        </defs>
        <g transform="translate(500,215) scale(1.2)">
          <g stroke-width="12" stroke-linejoin="round">
            <path class="animated-element tri-left" d="M -120 45 L -55 110 L -185 110 Z" fill="#136070" stroke="#136070" />
            <path class="animated-element tri-right" d="M 120 45 L 185 110 L 55 110 Z" fill="#207886" stroke="#207886" />
          </g>
          <g transform="rotate(45)" stroke-linejoin="round">
            <path class="animated-element piece-br" d="M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z" fill="url(#bottomDarkGrad)" filter="url(#pieceShadow)" />
            <path class="animated-element piece-bl" d="M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z" fill="#136070" filter="url(#pieceShadow)" />
            <path class="animated-element piece-tr" d="M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z" fill="#F1A463" filter="url(#pieceShadow)" />
            <path class="animated-element piece-tl" d="M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z" fill="url(#topBlueGrad)" filter="url(#pieceShadow)" />
          </g>
          <text class="animated-element text-title" x="500" y="470" font-family="'Montserrat','Helvetica Neue','Arial',sans-serif" font-weight="900" font-size="82" fill="#333333" text-anchor="middle" letter-spacing="6">TESSERA</text>
          <text class="animated-element text-sub" x="500" y="525" font-family="'Montserrat','Helvetica Neue','Arial',sans-serif" font-weight="400" font-size="26" fill="#4B555A" text-anchor="middle" letter-spacing="0.5">The foundational tiles of enterprise architecture</text>
        </g>
      </svg>
    `;
    this.shadowRoot.appendChild(template.content.cloneNode(true));
    // Trigger animation on intersection
    const svgEl = this.shadowRoot.getElementById('tessera-logo');
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          svgEl.classList.add('play-animation');
          observer.unobserve(svgEl);
        }
      });
    }, { threshold: 0.1 });
    observer.observe(svgEl);
  }
}

customElements.define('tessera-logo', TesseraLogo);
