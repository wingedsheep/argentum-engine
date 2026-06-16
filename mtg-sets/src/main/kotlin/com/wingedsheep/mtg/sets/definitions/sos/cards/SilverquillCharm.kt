package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Silverquill Charm
 * {W}{B}
 * Instant
 * Choose one —
 * • Put two +1/+1 counters on target creature.
 * • Exile target creature with power 2 or less.
 * • Each opponent loses 3 life and you gain 3 life.
 */
val SilverquillCharm = card("Silverquill Charm") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Put two +1/+1 counters on target creature.\n• Exile target creature with power 2 or less.\n• Each opponent loses 3 life and you gain 3 life."

    spell {
        modal(chooseCount = 1) {
            mode("Put two +1/+1 counters on target creature") {
                val t = target("target creature", TargetCreature())
                effect = Effects.AddCounters("+1+1", 2, t)
            }
            mode("Exile target creature with power 2 or less") {
                val t = target(
                    "target creature with power 2 or less",
                    TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerAtMost(2)))
                )
                effect = Effects.Exile(t)
            }
            mode("Each opponent loses 3 life and you gain 3 life") {
                effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Effects.GainLife(3, EffectTarget.Controller)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Ksenia Kim"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3eb73579-f1c6-4762-81d2-9568ab501fac.jpg?1775938570"
    }
}
