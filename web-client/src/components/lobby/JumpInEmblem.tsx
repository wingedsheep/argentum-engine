/** Two themed packs combined into one deck. Decorative, shared by the picker and lobby. */
export function JumpInEmblem({ className }: { className?: string | undefined }) {
  return (
    <svg className={className} viewBox="0 0 84 84" fill="none" aria-hidden="true" focusable="false">
      <g transform="rotate(-12 29 42)">
        <rect x="9" y="13" width="36" height="56" rx="5" fill="#183f46" stroke="#6fd3c0" strokeWidth="2" />
        <path d="m27 28 9 14-9 14-9-14Z" fill="#6fd3c0" />
      </g>
      <g transform="rotate(12 55 42)">
        <rect x="39" y="13" width="36" height="56" rx="5" fill="#493522" stroke="#efc477" strokeWidth="2" />
        <path d="m57 28 9 14-9 14-9-14Z" fill="#efc477" />
      </g>
    </svg>
  )
}
