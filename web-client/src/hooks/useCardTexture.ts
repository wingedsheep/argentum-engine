import { useState, useEffect } from 'react'
import { TextureLoader, Texture, SRGBColorSpace } from 'three'

/**
 * Cache for loaded textures to prevent reloading.
 */
const textureCache = new Map<string, Texture>()

/**
 * Generate Scryfall image URL for a card.
 *
 * @param cardName - The name of the card
 * @param size - Image size: 'small', 'normal', 'large', 'png', 'art_crop', 'border_crop'
 */
export function getScryfallImageUrl(
  cardName: string,
  size: 'small' | 'normal' | 'large' | 'png' | 'art_crop' | 'border_crop' = 'normal'
): string {
  const encodedName = encodeURIComponent(cardName)
  return `https://api.scryfall.com/cards/named?exact=${encodedName}&format=image&version=${size}`
}

/**
 * Placeholder image for card backs.
 */
export const CARD_BACK_URL = 'https://backs.scryfall.io/large/c1/09/c1092f8a-3cd9-46b5-a06e-bf8ab43e4bb9.jpg'

/**
 * Hook to load a card texture from Scryfall.
 *
 * Returns the texture if loaded, null if loading, or a fallback if failed.
 */
export function useCardTexture(cardName: string | null): Texture | null {
  const [texture, setTexture] = useState<Texture | null>(null)

  useEffect(() => {
    if (!cardName) {
      setTexture(null)
      return
    }

    const url = getScryfallImageUrl(cardName, 'normal')

    // Check cache first
    const cached = textureCache.get(url)
    if (cached) {
      setTexture(cached)
      return
    }

    const loader = new TextureLoader()
    loader.load(
      url,
      (loadedTexture) => {
        loadedTexture.colorSpace = SRGBColorSpace
        textureCache.set(url, loadedTexture)
        setTexture(loadedTexture)
      },
      undefined,
      () => {
        console.warn(`Failed to load texture for card: ${cardName}`)
      }
    )

    return () => {
      // Cleanup if component unmounts during load
    }
  }, [cardName])

  return texture
}

/**
 * Hook to load the card back texture.
 */
export function useCardBackTexture(): Texture | null {
  const [texture, setTexture] = useState<Texture | null>(null)

  useEffect(() => {
    const cached = textureCache.get(CARD_BACK_URL)
    if (cached) {
      setTexture(cached)
      return
    }

    const loader = new TextureLoader()
    loader.load(
      CARD_BACK_URL,
      (loadedTexture) => {
        loadedTexture.colorSpace = SRGBColorSpace
        textureCache.set(CARD_BACK_URL, loadedTexture)
        setTexture(loadedTexture)
      },
      undefined,
      () => {
        console.warn('Failed to load card back texture')
      }
    )
  }, [])

  return texture
}

/**
 * Preload textures for a list of card names.
 * Useful for loading hand/battlefield cards in advance.
 */
export function preloadCardTextures(cardNames: string[]): void {
  const loader = new TextureLoader()

  cardNames.forEach((name) => {
    const url = getScryfallImageUrl(name, 'normal')
    if (!textureCache.has(url)) {
      loader.load(
        url,
        (texture) => {
          texture.colorSpace = SRGBColorSpace
          textureCache.set(url, texture)
        },
        undefined,
        () => {
          console.warn(`Failed to preload texture for: ${name}`)
        }
      )
    }
  })
}

/**
 * Get the color for a card based on its colors.
 * Used as a fallback when texture isn't loaded.
 */
export function getCardFrameColor(colors: readonly string[]): string {
  if (colors.length === 0) return '#9e9e9e' // Colorless
  if (colors.length > 1) return '#d4af37' // Gold/Multicolor

  switch (colors[0]) {
    case 'WHITE': return '#f9faf4'
    case 'BLUE': return '#0e68ab'
    case 'BLACK': return '#150b00'
    case 'RED': return '#d3202a'
    case 'GREEN': return '#00733e'
    default: return '#9e9e9e'
  }
}
