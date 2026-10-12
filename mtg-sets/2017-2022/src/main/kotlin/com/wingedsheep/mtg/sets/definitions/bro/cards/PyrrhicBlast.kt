package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pyrrhic Blast — The Brothers' War #148
 * {3}{R} · Instant
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Pyrrhic Blast deals damage equal to the sacrificed creature's power to any target. Draw a card.
 */
val PyrrhicBlast = card("Pyrrhic Blast") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Pyrrhic Blast deals damage equal to the sacrificed creature's power to any target. Draw a card."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sacrificedPower(), t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Raf Kayupov"
        flavorText = "Only the quartermaster wept for their loss."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a523d5a7-58b1-45a3-81a7-078da02fda16.jpg?1783920061"
        ruling("2022-10-14", "The sacrificed creature's last known existence on the battlefield is checked to determine its power.")
    }
}
