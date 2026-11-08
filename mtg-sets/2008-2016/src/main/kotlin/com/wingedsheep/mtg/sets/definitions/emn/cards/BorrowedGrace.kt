package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Borrowed Grace — Eldritch Moon #14.
 *
 * Borrowed Hostility's escalate shape; each mode is Virtuous Charge's board-wide pump
 * ([Effects.ForEachInGroup] over `creaturesYouControl`), so it affects only the creatures you
 * control as the spell resolves.
 */
val BorrowedGrace = card("Borrowed Grace") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Escalate {1}{W} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or both —\n" +
        "• Creatures you control get +2/+0 until end of turn.\n" +
        "• Creatures you control get +0/+2 until end of turn."

    spell {
        modal(chooseCount = 2, minChooseCount = 1, additionalManaCostPerExtraMode = "{1}{W}") {
            mode("Creatures you control get +2/+0 until end of turn.") {
                effect = Effects.ForEachInGroup(
                    Filters.Group.creaturesYouControl,
                    Effects.ModifyStats(2, 0, EffectTarget.IterationEntity)
                )
            }
            mode("Creatures you control get +0/+2 until end of turn.") {
                effect = Effects.ForEachInGroup(
                    Filters.Group.creaturesYouControl,
                    Effects.ModifyStats(0, 2, EffectTarget.IterationEntity)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0067567-3434-4c12-9d4d-04ffc98d012c.jpg?1783937525"
        ruling("2016-07-13", "You choose all of your modes at once. You can't wait to perform one mode's actions and then decide to choose more modes.")
        ruling("2016-07-13", "Effects that reduce the cost of spells reduce the total cost, including any escalate costs added.")
        ruling("2016-07-13", "If an effect allows you to cast a spell that has escalate without paying its mana cost, you pay escalate costs for that spell if you choose more than one mode.")
    }
}
