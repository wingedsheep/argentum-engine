package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Hear Something on Watch
 * {1}{W}
 * Instant
 *
 * Choose one —
 * • Rouse the Party — Creatures you control get +1/+1 until end of turn.
 * • Set Off Traps — This spell deals 5 damage to target attacking creature.
 *
 * A plain choose-one `modal`: the untargeted team pump (Defend the Campus's first mode) and a
 * 5-damage burn restricted to an attacking creature.
 */
val YouHearSomethingOnWatch = card("You Hear Something on Watch") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Rouse the Party — Creatures you control get +1/+1 until end of turn.\n" +
        "• Set Off Traps — This spell deals 5 damage to target attacking creature."

    spell {
        modal(chooseCount = 1) {
            mode(
                "Rouse the Party — Creatures you control get +1/+1 until end of turn.",
                Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
            )
            mode("Set Off Traps — This spell deals 5 damage to target attacking creature.") {
                val attacker = target(TargetFilter.AttackingCreature)
                effect = Effects.DealDamage(5, attacker)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76e939ab-9d0c-4685-805c-c8bc4e6af163.jpg?1783926521"
    }
}
