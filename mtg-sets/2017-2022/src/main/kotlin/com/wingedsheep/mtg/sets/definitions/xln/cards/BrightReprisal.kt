package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bright Reprisal
 * {4}{W}
 * Instant
 * Destroy target attacking creature.
 * Draw a card.
 */
val BrightReprisal = card("Bright Reprisal") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target attacking creature.\nDraw a card."

    spell {
        val victim = target(TargetFilter.AttackingCreature)
        effect = Effects.Destroy(victim) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3340ffb9-9513-4551-ad64-821600596b2e.jpg?1783935805"
    }
}
