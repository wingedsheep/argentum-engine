package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Flumph — Adventures in the Forgotten Realms #15
 * {1}{W} · Creature — Jellyfish · 0/4
 *
 * Defender, flying
 * Whenever this creature is dealt damage, you and target opponent each draw a card.
 */
val Flumph = card("Flumph") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Jellyfish"
    power = 0
    toughness = 4
    oracleText = "Defender, flying\n" +
        "Whenever this creature is dealt damage, you and target opponent each draw a card."

    keywords(Keyword.DEFENDER, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(1) then Effects.DrawCards(1, opponent)
        description = "Whenever this creature is dealt damage, you and target opponent each draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Brian Valeza"
        flavorText = "Flumphs siphon thoughts and energy from evil aberrations, and are often eager to share the dark knowledge they obtain with friendly adventurers."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdc86e78-8911-4a0d-ba3a-7802f8d991ef.jpg?1783926532"
    }
}
