package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Wilds of Eldraine's Rat token: a 1/1 black Rat creature token with "This token can't block."
 *
 * The Rat is a type-named token (no printed name beyond its creature type), so it goes through
 * [Effects.CreateToken] rather than a [com.wingedsheep.mtg.sets.tokens.PredefinedTokens] entry.
 * Several WOE cards create it (Harried Spearguard, Warehouse Tabby, Rat Out, …), so the shape —
 * including the token's art — lives here rather than being copied per card.
 */
internal fun woeRatToken(): Effect = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.BLACK),
    creatureTypes = setOf("Rat"),
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1e0205f2-25c1-403b-b408-56e3f2d63b4d.jpg?1783915000",
    staticAbilities = listOf(CantBlock())
)
