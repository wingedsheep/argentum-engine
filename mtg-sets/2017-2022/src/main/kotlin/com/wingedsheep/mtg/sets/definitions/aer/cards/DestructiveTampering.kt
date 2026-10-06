package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Destructive Tampering
 * {2}{R}
 * Sorcery
 * Choose one —
 * • Destroy target artifact.
 * • Creatures without flying can't block this turn.
 *
 * Mode 2 is a dynamic group restriction (CR 611.2c), so creatures without flying that enter
 * later in the turn can't block either — matching the printed ruling.
 */
val DestructiveTampering = card("Destructive Tampering") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Creatures without flying can't block this turn."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact))
                effect = Effects.Destroy(artifact)
            }
            mode("Creatures without flying can't block this turn") {
                effect = Effects.CantBlockGroup(
                    GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Titus Lunter"
        flavorText = "\"I don't think they'll appreciate my . . . adjustments.\"\n—Karavin, renegade saboteur"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00154b70-57d2-4c32-860f-1c36fc49b10c.jpg?1783936756"
        ruling(
            "2020-06-23",
            "Because the effect of Destructive Tampering's second mode doesn't change the characteristics of any permanents, the set of creatures affected by it is constantly updated. Creatures without flying that enter the battlefield later in the turn won't be able to block."
        )
    }
}
