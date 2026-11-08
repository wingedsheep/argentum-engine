/**
 * The Jumpstart sets Jump In can deal packs from — the ones the server has published pack lists for
 * (`JumpstartPacks.SUPPORTED_SETS`). Newest first; the first is the default.
 *
 * Label + year are short on purpose ("Jumpstart · 2022"): the set's full name is shown under the
 * choice, and every option has to fit one segmented control on a phone.
 */
export const JUMPSTART_SETS: readonly { readonly code: string; readonly label: string; readonly year: string }[] = [
  { code: 'J25', label: 'Foundations', year: '2024' },
  { code: 'J22', label: 'Jumpstart', year: '2022' },
  { code: 'JMP', label: 'Original', year: '2020' },
]

export const DEFAULT_JUMPSTART_SET = JUMPSTART_SETS[0]!.code

export function isJumpstartSet(code: string | null | undefined): boolean {
  return JUMPSTART_SETS.some((s) => s.code === code)
}
