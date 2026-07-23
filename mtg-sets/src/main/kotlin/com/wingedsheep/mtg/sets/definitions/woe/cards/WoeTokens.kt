package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wilds of Eldraine's Rat token: a 1/1 black Rat creature token with "This token can't block."
 *
 * The Rat is a type-named token (no printed name beyond its creature type), so it goes through
 * [Effects.CreateToken] rather than a [com.wingedsheep.mtg.sets.tokens.PredefinedTokens] entry.
 * Several WOE cards create it (Harried Spearguard, Warehouse Tabby, Rat Out, …), so the shape —
 * including the token's art — lives here rather than being copied per card.
 *
 * [count] is a [DynamicAmount] so the same helper covers the fixed-count cards and Song of
 * Totentanz's "create X … Rat creature tokens".
 */
internal fun woeRatToken(count: DynamicAmount = DynamicAmount.Fixed(1)): Effect = Effects.CreateToken(
    count = count,
    power = 1,
    toughness = 1,
    colors = setOf(Color.BLACK),
    creatureTypes = setOf("Rat"),
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1e0205f2-25c1-403b-b408-56e3f2d63b4d.jpg?1783915000",
    staticAbilities = listOf(CantBlock())
)

/**
 * Wilds of Eldraine's Faerie token: a 1/1 blue Faerie creature token with flying that can block
 * only creatures with flying.
 */
internal fun woeFaerieToken(count: DynamicAmount = DynamicAmount.Fixed(1)): Effect = Effects.CreateToken(
    count = count,
    power = 1,
    toughness = 1,
    colors = setOf(Color.BLUE),
    creatureTypes = setOf("Faerie"),
    keywords = setOf(Keyword.FLYING),
    imageUri = "https://cards.scryfall.io/normal/front/0/f/0f9a993f-1f2b-4b17-b415-e975b7873e18.jpg?1783914992",
    staticAbilities = listOf(
        CanOnlyBlockCreaturesWith(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        )
    )
)
