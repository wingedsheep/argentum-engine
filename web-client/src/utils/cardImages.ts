// The three face-down helper cards below mirror `FaceDownMode.helperCardImageUri` in `mtg-sdk`: the
// server sends the same URL as a masked face-down card's `imageUri`, and these constants cover the
// surfaces the client draws from the mode alone. Keep the two lists in sync.

/**
 * Standard MTG morph face-down card art from Scryfall.
 * This is the official morph token from Commander 2019 (TC19 #27) showing the distinctive helmet artwork.
 * Source: https://scryfall.com/card/tc19/27/morph
 */
export const MORPH_FACE_DOWN_IMAGE_URL = 'https://cards.scryfall.io/normal/front/e/9/e9375cbe-93c0-41a5-a6e3-fb4416f54a69.jpg'

/**
 * Standard MTG manifest face-down card art from Scryfall.
 * The official Manifest token from Duskmourn: House of Horror (TDSK #18). Manifested permanents
 * (CR 701.40) are shown with this instead of the morph token.
 * Source: https://scryfall.com/card/tdsk/18/manifest
 */
export const MANIFEST_FACE_DOWN_IMAGE_URL = 'https://cards.scryfall.io/normal/front/0/1/01104ab1-84e1-4c78-853d-637c6554bdf9.jpg'

/**
 * Face-down card art for disguise (CR 702.168) and cloak (CR 701.58).
 *
 * "A Mysterious Creature" is the single helper card printed for *both* mechanics — its own reminder
 * text reads "a face-down creature that was cloaked or cast with disguise has ward {2}" — so paper
 * Magic covers either with this one card and so do we. Taken from the Murders at Karlov Manor
 * printing (TMKM #21, Ben Hill), the set that introduced disguise and cloak; Assassin's Creed
 * reprinted the same card with different art (TACR #8).
 * Source: https://scryfall.com/card/tmkm/21/a-mysterious-creature
 */
export const WARDED_FACE_DOWN_IMAGE_URL = 'https://cards.scryfall.io/normal/front/2/4/241b3b6d-a25f-4a43-b5d6-1d1079e7e498.jpg'

/**
 * Face-down helper-card art per {@link ClientCard.faceDownMode}. Falls back to the morph token,
 * which is what an unmarked face-down permanent has always been rendered as.
 */
export function faceDownImageUrl(faceDownMode?: string): string {
  switch (faceDownMode) {
    case 'MANIFEST':
      return MANIFEST_FACE_DOWN_IMAGE_URL
    case 'DISGUISE':
    case 'CLOAK':
      return WARDED_FACE_DOWN_IMAGE_URL
    default:
      return MORPH_FACE_DOWN_IMAGE_URL
  }
}

/**
 * Standard MTG card back image.
 */
export const CARD_BACK_IMAGE_URL = 'https://backs.scryfall.io/normal/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389'

/**
 * Degrees to rotate a card's hover preview image: 90° for a card that is **printed sideways**,
 * 0 otherwise.
 *
 * Which cards those are is decided server-side, in `CardDefinition.isLandscapePrint` — split
 * layouts (Rooms, Pain // Suffering) and battles (CR 310). Prefer the `isLandscape` flag the
 * server sends; the `layout` / `typeLine` derivation below is only a fallback for card shapes that
 * predate the flag, and exists so no surface silently reverts to upright.
 */
export function landscapeImageRotateDeg(
  card: { isLandscape?: boolean; layout?: string; typeLine?: string | null } | null | undefined
): 0 | 90 {
  if (!card) return 0
  if (card.isLandscape !== undefined) return card.isLandscape ? 90 : 0
  if (card.layout === 'SPLIT') return 90
  return isBattleTypeLine(card.typeLine) ? 90 : 0
}

/**
 * Whether a printed type line names the Battle card type (CR 310). Only the types half of the line
 * is examined — everything before the em dash — so a subtype or a card name can never match.
 * Fallback only: prefer the server's `isLandscape` flag.
 */
export function isBattleTypeLine(typeLine: string | null | undefined): boolean {
  if (!typeLine) return false
  const types = typeLine.split('—')[0] ?? ''
  return /\bBattle\b/.test(types)
}

/**
 * Get the image URL for a card.
 *
 * Uses the provided imageUri if available (from card metadata),
 * otherwise falls back to Scryfall API lookup by card name.
 *
 * @param cardName The card's name (used for Scryfall fallback)
 * @param imageUri The card's direct image URI from metadata (optional)
 * @param version The image version/size to request
 * @returns The image URL to use
 */
export function getCardImageUrl(
  cardName: string,
  imageUri?: string | null,
  version: 'small' | 'normal' | 'large' = 'normal'
): string {
  if (imageUri) {
    return imageUri
  }
  return getScryfallFallbackUrl(cardName, version)
}

/**
 * Get a Scryfall API fallback URL for a card image.
 *
 * @param cardName The card's name
 * @param version The image version/size to request
 * @returns The Scryfall API image URL
 */
export function getScryfallFallbackUrl(
  cardName: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): string {
  // Token names have a " Token" suffix (e.g., "Insect Token") that Scryfall doesn't use
  const scryfallName = cardName.endsWith(' Token') ? cardName.slice(0, -6) : cardName
  return `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(scryfallName)}&format=image&version=${version}`
}

/**
 * Get the landscape *art crop* for a card by name (Scryfall `version=art_crop`).
 *
 * Unlike the full-card images above, this returns just the illustration with no frame,
 * which is the right shape for a wide banner/tile background. Used by the saved-deck
 * gallery to paint each deck's hero art from its rarest card.
 *
 * @param cardName The card's name (the default printing's art is used)
 */
export function getScryfallArtCropUrl(cardName: string): string {
  const scryfallName = cardName.endsWith(' Token') ? cardName.slice(0, -6) : cardName
  return `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(scryfallName)}&format=image&version=art_crop`
}

/**
 * Derive the landscape *art crop* directly from a card's stored image URL.
 *
 * Our card metadata already carries a direct `cards.scryfall.io` CDN URL in `imageUri`
 * (almost always the `normal` size). Scryfall keys every size of an image under the same
 * path with only the size segment differing (`small` | `normal` | `large` | `png` |
 * `border_crop` | `art_crop`), so swapping that segment yields the crop on the same CDN
 * host — no `api.scryfall.com` round-trip, no redirect, and no API rate limiting (which
 * only applies to the API host, not the image CDN).
 *
 * Prefer this over {@link getScryfallArtCropUrl} whenever a card's `imageUri` is on hand;
 * fall back to the by-name API lookup only when it isn't (returns null here).
 *
 * @param imageUri A card's `imageUri` (e.g. `https://cards.scryfall.io/normal/front/a/b/<id>.jpg`)
 * @returns The CDN `art_crop` URL, or null when `imageUri` isn't a recognised Scryfall CDN image URL
 */
export function getCdnArtCropUrl(imageUri: string | null | undefined): string | null {
  if (!imageUri) return null
  const match = imageUri.match(
    /^(https:\/\/cards\.scryfall\.io\/)(small|normal|large|png|border_crop)(\/.*?)(\.\w+)(\?.*)?$/
  )
  if (!match) return null
  const [, host, , path, , query = ''] = match
  // art_crop is always served as .jpg, even when the source size (e.g. png) isn't.
  return `${host}art_crop${path}.jpg${query}`
}
