/**
 * Standard MTG morph face-down card art from Scryfall.
 * This is the official morph token from Commander 2019 (TC19 #27) showing the distinctive helmet artwork.
 * Source: https://scryfall.com/card/tc19/27/morph
 */
export const MORPH_FACE_DOWN_IMAGE_URL = 'https://cards.scryfall.io/large/front/e/9/e9375cbe-93c0-41a5-a6e3-fb4416f54a69.jpg'

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
  return `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=${version}`
}
