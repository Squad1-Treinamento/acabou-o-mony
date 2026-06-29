"use client";

import { useEffect, useRef } from "react";

// ── Laptop ────────────────────────────────────────────────────────────────────
const L_W  = 480;   // screen width
const L_H  = 300;   // screen height
const L_CW = 1280;  // content viewport
const L_SC = L_W / L_CW;

// ── Phone ─────────────────────────────────────────────────────────────────────
const P_FW = 178;   // frame width
const P_FH = 368;   // frame height
const P_SW = 166;   // screen width  (6px bezel each side)
const P_CW = 390;   // content viewport
const P_SC = P_SW / P_CW;

const STATUS_H  = 46;   // black status bar (includes Dynamic Island)
const BOTTOM_H  = 22;   // black home bar
const P_CH      = P_FH - 20 - STATUS_H - BOTTOM_H; // visible content height
const P_TOP     = (P_FH - (STATUS_H + P_CH + BOTTOM_H)) / 2; // top offset inside frame

function injectNoScroll(el: HTMLIFrameElement) {
  try {
    const doc = el.contentDocument ?? el.contentWindow?.document;
    if (!doc) return;
    const s = doc.createElement("style");
    s.textContent = "html,body{overflow:hidden!important;scrollbar-width:none!important;}::-webkit-scrollbar{display:none!important;}";
    doc.head.appendChild(s);
  } catch {}
}

export function DeviceMockups() {
  const laptopRef = useRef<HTMLIFrameElement>(null);
  const phoneRef  = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    [laptopRef, phoneRef].forEach(({ current: el }) => {
      if (!el) return;
      el.addEventListener("load", () => injectNoScroll(el));
    });
  }, []);

  return (
    <div className="relative select-none" style={{ width: 580, height: 440 }}>

      {/* ══════════════════ MacBook ══════════════════════════════════════════ */}
      <div className="absolute left-0 top-0" style={{ filter: "drop-shadow(0 20px 40px rgba(0,0,0,0.35))" }}>
        {/* Lid */}
        <div style={{
          width: L_W + 20, borderRadius: "14px 14px 0 0",
          padding: "8px 8px 0",
          background: "linear-gradient(180deg,#ddd 0%,#c6c6c6 100%)",
          boxShadow: "inset 0 1px 0 rgba(255,255,255,0.6)",
        }}>
          <div style={{ borderRadius: "8px 8px 0 0", overflow: "hidden", background: "#111", paddingTop: 14 }}>
            <div style={{ display: "flex", justifyContent: "center", marginBottom: 6 }}>
              <div style={{ width: 6, height: 6, borderRadius: "50%", background: "#2a2a2a", boxShadow: "0 0 0 1.5px #444" }} />
            </div>
            <div style={{ width: L_W, height: L_H, overflow: "hidden", background: "#fff" }}>
              <iframe ref={laptopRef} src="/checkout-preview" title="Desktop" style={{
                width: L_CW, height: Math.ceil(L_H / L_SC), border: "none",
                transformOrigin: "top left", transform: `scale(${L_SC})`,
                pointerEvents: "none", display: "block",
              }} />
            </div>
          </div>
        </div>
        {/* Hinge */}
        <div style={{ width: L_W + 20, height: 4, background: "linear-gradient(180deg,#b0b0b0,#c0c0c0)", boxShadow: "0 1px 0 #aaa" }} />
        {/* Base */}
        <div style={{
          width: L_W + 36, marginLeft: -8, height: 18,
          borderRadius: "0 0 8px 8px",
          background: "linear-gradient(180deg,#c8c8c8,#b4b4b4)",
          boxShadow: "0 5px 14px rgba(0,0,0,0.28), inset 0 1px 0 rgba(255,255,255,0.35)",
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>
          <div style={{ width: 56, height: 7, borderRadius: 4, background: "rgba(0,0,0,0.07)", border: "1px solid rgba(0,0,0,0.09)" }} />
        </div>
        <div style={{ width: L_W + 36, marginLeft: -8, height: 3, borderRadius: "0 0 4px 4px", background: "#9a9a9a" }} />
      </div>

      {/* ══════════════════ Phone — frame fino preto ═════════════════════════ */}
      <div className="absolute bottom-0 right-0 z-10" style={{
        width: P_FW,
        height: P_FH,
        borderRadius: 38,
        border: "3px solid #111",
        background: "#111",
        boxShadow: "0 24px 60px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.06)",
        overflow: "hidden",
        position: "absolute",
      }}>

        {/* Status bar */}
        <div style={{ height: STATUS_H, background: "#000", display:"flex", alignItems:"center", justifyContent:"center", position:"relative", flexShrink: 0 }}>
          {/* Dynamic Island */}
          <div style={{ width: 88, height: 26, borderRadius: 20, background: "#000", border: "1px solid #222", display:"flex", alignItems:"center", justifyContent:"center", gap: 6 }}>
            <div style={{ width: 26, height: 4, borderRadius: 3, background: "#1c1c1c" }} />
            <div style={{ width: 8, height: 8, borderRadius: "50%", background: "#0e0e0e", border: "1.5px solid #1c1c1c" }} />
          </div>
          <span style={{ position:"absolute", left:12, fontSize:8.5, fontWeight:700, color:"rgba(255,255,255,0.85)", fontFamily:"system-ui" }}>9:41</span>
          <div style={{ position:"absolute", right:12, display:"flex", alignItems:"center", gap:3 }}>
            <div style={{ display:"flex", alignItems:"flex-end", gap:1 }}>
              {[4,6,8,10].map((h,i)=><div key={i} style={{ width:2, height:h, borderRadius:1, background:`rgba(255,255,255,${i<3?0.85:0.3})` }} />)}
            </div>
            <div style={{ width:14, height:7, borderRadius:2, border:"1px solid rgba(255,255,255,0.4)", display:"flex", alignItems:"center", padding:"1px" }}>
              <div style={{ height:"100%", width:"70%", borderRadius:1, background:"rgba(255,255,255,0.85)" }} />
            </div>
          </div>
        </div>

        {/* Content */}
        <div style={{ width: P_SW, height: P_CH, overflow: "hidden", margin: "0 auto" }}>
          <iframe
            ref={phoneRef}
            src="/checkout-preview"
            title="Mobile"
            style={{
              width: P_CW,
              height: Math.ceil(P_CH / P_SC),
              border: "none",
              transformOrigin: "top left",
              transform: `scale(${P_SC})`,
              pointerEvents: "none",
              display: "block",
            }}
          />
        </div>

        {/* Home bar */}
        <div style={{ height: BOTTOM_H, background: "#000", display:"flex", alignItems:"center", justifyContent:"center" }}>
          <div style={{ width: 80, height: 4, borderRadius: 4, background: "rgba(255,255,255,0.25)" }} />
        </div>

      </div>

    </div>
  );
}
