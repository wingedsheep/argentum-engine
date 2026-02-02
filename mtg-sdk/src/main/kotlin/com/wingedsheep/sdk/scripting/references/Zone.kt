package com.wingedsheep.sdk.scripting

/**
 * Re-export Zone enum from events/ for the unified references API.
 *
 * The canonical Zone enum is defined in events/Zone.kt.
 * This file provides consistent imports from the references/ package.
 *
 * Zone values:
 * - Battlefield - The main play area
 * - Graveyard - Discard pile
 * - Hand - Cards in a player's hand
 * - Library - A player's deck
 * - Exile - Removed from game
 * - Stack - Spells and abilities being resolved
 * - Command - Command zone (commanders, emblems)
 *
 * @see com.wingedsheep.sdk.scripting.Zone
 */

// The Zone enum is already defined in events/Zone.kt with package com.wingedsheep.sdk.scripting
// No need to redefine it - just document that it's available from this package
