package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Render Inert (March of the Machine #123)
 * {2}{B} Sorcery
 * Remove up to five counters from target permanent.
 * Draw a card.
 *
 * [Effects.RemoveCountersUpTo] is the budget-capped counter removal: the caster picks how many of
 * each kind come off, five in total across all kinds, and may remove none.
 */
val RenderInert = card("Render Inert") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Remove up to five counters from target permanent.\nDraw a card."

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.RemoveCountersUpTo(5, permanent) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Yigit Koroglu"
        flavorText = "As New Phyrexia phased out of existence, glistening oil all across the Multiverse " +
            "was cut off from its source. The *compleated* invaders ceased to function and crumpled to the ground."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3b66ef5-9b93-4042-a618-c1ade83d0004.jpg?1783917002"
    }
}
