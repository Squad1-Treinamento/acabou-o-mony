import type { FC } from "react";

export type CardBrandKey = "visa" | "mastercard" | "amex" | "elo";

function VisaIcon() {
  return (
    <svg viewBox="0 0 48 30" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="48" height="30" rx="4" fill="#1A1F71" />
      {/* Gold stripe */}
      <rect x="0" y="22" width="48" height="5" rx="0" fill="#F7B600" />
      <rect x="0" y="22" width="48" height="5" rx="0" fill="url(#visaGold)" />
      <text
        x="24" y="17"
        fill="white"
        fontSize="12"
        fontWeight="800"
        textAnchor="middle"
        fontStyle="italic"
        fontFamily="Arial, sans-serif"
        letterSpacing="1"
      >
        VISA
      </text>
      <defs>
        <linearGradient id="visaGold" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#F7C948" />
          <stop offset="100%" stopColor="#E5A000" />
        </linearGradient>
      </defs>
    </svg>
  );
}

function MastercardIcon() {
  return (
    <svg viewBox="0 0 48 30" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="48" height="30" rx="4" fill="#252525" />
      {/* Red circle */}
      <circle cx="18" cy="15" r="9" fill="#EB001B" />
      {/* Orange/yellow circle */}
      <circle cx="30" cy="15" r="9" fill="#F79E1B" />
      {/* Orange overlap lens — computed intersection of cx=18,r=9 and cx=30,r=9 at y center=15 */}
      <path d="M24 8 A9 9 0 0 0 24 22 A9 9 0 0 0 24 8Z" fill="#FF5F00" />
    </svg>
  );
}

function AmexIcon() {
  return (
    <svg viewBox="0 0 48 30" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="48" height="30" rx="4" fill="#2E77BC" />
      {/* Blue gradient sheen */}
      <rect width="48" height="30" rx="4" fill="url(#amexSheen)" />
      <text
        x="24" y="19"
        fill="white"
        fontSize="9"
        fontWeight="700"
        textAnchor="middle"
        fontFamily="Arial, sans-serif"
        letterSpacing="1.5"
      >
        AMEX
      </text>
      <defs>
        <linearGradient id="amexSheen" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="white" stopOpacity="0.08" />
          <stop offset="100%" stopColor="white" stopOpacity="0" />
        </linearGradient>
      </defs>
    </svg>
  );
}

function EloIcon() {
  return (
    <svg viewBox="0 0 48 30" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="48" height="30" rx="4" fill="white" />
      <rect width="48" height="30" rx="4" stroke="#E5E7EB" strokeWidth="0.75" />
      {/* Elo colored accent dots */}
      <circle cx="10" cy="15" r="3.5" fill="#FFD400" />
      <circle cx="19" cy="15" r="3.5" fill="#00A3E0" />
      <circle cx="28" cy="15" r="3.5" fill="#F04E23" />
      <text
        x="40" y="19"
        fill="#1A1A1A"
        fontSize="8"
        fontWeight="700"
        textAnchor="middle"
        fontFamily="Arial, sans-serif"
        letterSpacing="0"
      >
        elo
      </text>
    </svg>
  );
}

const BRAND_MAP: Record<CardBrandKey, FC> = {
  visa: VisaIcon,
  mastercard: MastercardIcon,
  amex: AmexIcon,
  elo: EloIcon,
};

const ALL_BRANDS: CardBrandKey[] = ["visa", "mastercard", "amex", "elo"];

interface AcceptedBrandsProps {
  activeBrand?: CardBrandKey | "";
}

export function AcceptedBrands({ activeBrand }: AcceptedBrandsProps) {
  return (
    <div className="flex items-center gap-1.5">
      {ALL_BRANDS.map((brand) => {
        const Icon = BRAND_MAP[brand];
        const dimmed = activeBrand && activeBrand !== brand;
        return (
          <div
            key={brand}
            className={`w-9 transition-opacity duration-200 ${dimmed ? "opacity-20" : "opacity-100"}`}
          >
            <Icon />
          </div>
        );
      })}
    </div>
  );
}
