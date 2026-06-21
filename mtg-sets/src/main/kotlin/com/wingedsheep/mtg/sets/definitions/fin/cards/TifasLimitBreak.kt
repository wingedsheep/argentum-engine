package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tifa's Limit Break
 * {G}
 * Instant
 *
 * Tiered (Choose one additional cost.)
 * • Somersault — {0} — Target creature gets +2/+2 until end of turn.
 * • Meteor Strikes — {2} — Double target creature's power and toughness until end of turn.
 * • Final Heaven — {6}{G} — Triple target creature's power and toughness until end of turn.
 *
 * Tiered (CR 702.183): a choose-one modal spell where the chosen tier's additional mana cost is
 * paid at cast. Doubling/tripling is modeled as a layer-7 +N/+N modification read from the
 * target's own power/toughness as the effect begins to apply (CR 702 "double its power and
 * toughness" ruling): double = +P/+T, triple = +2P/+2T.
 */
val TifasLimitBreak = card("Tifa's Limit Break") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Tiered (Choose one additional cost.)\n" +
        "• Somersault — {0} — Target creature gets +2/+2 until end of turn.\n" +
        "• Meteor Strikes — {2} — Double target creature's power and toughness until end of turn.\n" +
        "• Final Heaven — {6}{G} — Triple target creature's power and toughness until end of turn."

    spell {
        tiered {
            tier("Somersault", "{0}", "Target creature gets +2/+2 until end of turn.") {
                effect = Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))
                target = Targets.Creature
            }
            tier("Meteor Strikes", "{2}", "Double target creature's power and toughness until end of turn.") {
                effect = Effects.ModifyStats(
                    power = DynamicAmounts.targetPower(0),
                    toughness = DynamicAmounts.targetToughness(0),
                    target = EffectTarget.ContextTarget(0)
                )
                target = Targets.Creature
            }
            tier("Final Heaven", "{6}{G}", "Triple target creature's power and toughness until end of turn.") {
                effect = Effects.ModifyStats(
                    power = DynamicAmount.Multiply(DynamicAmounts.targetPower(0), 2),
                    toughness = DynamicAmount.Multiply(DynamicAmounts.targetToughness(0), 2),
                    target = EffectTarget.ContextTarget(0)
                )
                target = Targets.Creature
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Mikio Masuda"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24d6eab7-22bd-494f-8cbe-204446f24be9.jpg?1748706536"
    }
}
