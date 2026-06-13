/**
 * Seat identity colors for multiplayer (3-4 player) games.
 *
 * Four fixed, colorblind-safe hues (Okabe-Ito palette) assigned by seat index
 * (= index in ClientGameState.players, which the server orders by turn order).
 * Used consistently everywhere a player's identity matters in a pod: opponent
 * rail chips, viewed-board edge flash, combat arrows, stack item borders, log
 * entry names. One legend, everywhere.
 *
 * Only surfaced in games with more than two players — the 2-player UI keeps its
 * existing your-vs-opponent color language untouched.
 */
export interface SeatColor {
  /** Main identity color */
  base: string
  /** Brighter variant for rings/glows */
  bright: string
  /** Translucent variant for surfaces/washes */
  soft: string
}

export const SEAT_COLORS: readonly SeatColor[] = [
  { base: '#E69F00', bright: '#FFC846', soft: 'rgba(230, 159, 0, 0.22)' }, // amber
  { base: '#56B4E9', bright: '#8ED4FF', soft: 'rgba(86, 180, 233, 0.22)' }, // sky blue
  { base: '#CC79A7', bright: '#F0A3CF', soft: 'rgba(204, 121, 167, 0.22)' }, // orchid
  { base: '#009E73', bright: '#2FD1A4', soft: 'rgba(0, 158, 115, 0.22)' }, // teal
]

/** Color for a seat index (wraps for safety; pods are capped at 4 seats). */
export function seatColor(seatIndex: number): SeatColor {
  const n = SEAT_COLORS.length
  return SEAT_COLORS[((seatIndex % n) + n) % n]!
}
