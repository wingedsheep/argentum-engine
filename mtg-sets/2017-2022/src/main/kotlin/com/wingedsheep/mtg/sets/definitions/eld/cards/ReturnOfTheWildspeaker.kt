package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Return of the Wildspeaker — Throne of Eldraine #172
 * {4}{G} · Instant
 *
 * Choose one —
 * • Draw cards equal to the greatest power among non-Human creatures you control.
 * • Non-Human creatures you control get +3/+3 until end of turn.
 *
 * Mode one reads the greatest power at resolution (Bouncer's Beatdown's `maxPower()` over a
 * battlefield count, here narrowed to non-Humans); mode two is a group pump.
 */
private val nonHumanCreatures = GameObjectFilter.Creature.notSubtype(Subtype("Human"))

val ReturnOfTheWildspeaker = card("Return of the Wildspeaker") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Draw cards equal to the greatest power among non-Human creatures you control.\n" +
        "• Non-Human creatures you control get +3/+3 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Draw cards equal to the greatest power among non-Human creatures you control") {
                effect = Effects.DrawCards(
                    DynamicAmounts.battlefield(Player.You, nonHumanCreatures).maxPower()
                )
            }
            mode("Non-Human creatures you control get +3/+3 until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(3, 3, GroupFilter(nonHumanCreatures.youControl()))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Chris Rallis"
        flavorText = "\"The curse is broken.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b88a4943-bd1b-4d10-9cd3-b2ab91b25c10.jpg?1783932604"
    }
}
