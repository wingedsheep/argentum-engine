/** Selectors for game board elements using data attributes and ARIA patterns. */

/** Card element by entity ID */
export const card = (id: string) => `[data-card-id="${id}"]`

/** Card image by card name (alt text) */
export const cardByName = (name: string) => `img[alt="${name}"]`

/** Player's hand zone */
export const HAND = '[data-zone="hand"]'

/** Opponent's hand zone */
export const OPPONENT_HAND = '[data-zone="opponent-hand"]'

/** Player life display by player ID */
export const lifeDisplay = (playerId: string) => `[data-life-display="${playerId}"]`

/** Player element by player ID */
export const playerById = (playerId: string) => `[data-player-id="${playerId}"]`

/** Graveyard pile by player ID */
export const graveyard = (playerId: string) => `[data-graveyard-id="${playerId}"]`

/** Exile pile by player ID */
export const exile = (playerId: string) => `[data-exile-id="${playerId}"]`

/** Battlefield zones */
export const BATTLEFIELD =
  '[data-zone="player-battlefield"], [data-zone="opponent-battlefield"]'

/** Library pile */
export const PLAYER_LIBRARY = '[data-zone="player-library"]'
export const OPPONENT_LIBRARY = '[data-zone="opponent-library"]'
