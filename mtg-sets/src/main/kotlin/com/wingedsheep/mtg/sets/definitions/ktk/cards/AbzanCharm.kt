package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Abzan Charm
 * {W}{B}{G}
 * Instant
 * Choose one —
 * • Exile target creature with power 3 or greater.
 * • You draw two cards and you lose 2 life.
 * • Distribute two +1/+1 counters among one or two target creatures.
 */
val AbzanCharm = card("Abzan Charm") {
    manaCost = "{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Exile target creature with power 3 or greater.\n• You draw two cards and you lose 2 life.\n• Distribute two +1/+1 counters among one or two target creatures."

    spell {
        modal(chooseCount = 1) {
            mode("Exile target creature with power 3 or greater") {
                val t = target("target", TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerAtLeast(3))))
                effect = Effects.Move(t, Zone.EXILE)
            }
            mode("You draw two cards and you lose 2 life") {
                effect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
            }
            mode("Distribute two +1/+1 counters among one or two target creatures") {
                target("target", TargetCreature(count = 2, minCount = 1))
                effect = Effects.DistributeCountersAmongTargets(totalCounters = 2)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f7b7598-35b0-4bb5-8347-8c868500f846.jpg?1562790259"
        ruling("2014-09-20", "If you choose the third mode, you choose how the counters will be distributed as you cast the spell. If one of the targets becomes illegal, the counters that would have been placed on that creature are lost.")
    }
}
