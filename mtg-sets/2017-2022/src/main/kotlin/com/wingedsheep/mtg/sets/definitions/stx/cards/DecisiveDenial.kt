package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decisive Denial
 * {G}{U}
 * Instant
 * Choose one —
 * • Target creature you control fights target creature you don't control.
 * • Counter target noncreature spell unless its controller pays {3}.
 */
val DecisiveDenial = card("Decisive Denial") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature you control fights target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "• Counter target noncreature spell unless its controller pays {3}."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature you control fights target creature you don't control") {
                val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
                val theirCreature = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
                effect = Effects.Fight(yourCreature, theirCreature)
            }
            mode("Counter target noncreature spell unless its controller pays {3}") {
                target(TargetFilter.NoncreatureSpellOnStack)
                effect = Effects.CounterUnlessPays("{3}")
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"I've heard enough.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2e9d132-95f7-4ee7-9c91-be19e4ad7a5d.jpg?1783927318"
    }
}
