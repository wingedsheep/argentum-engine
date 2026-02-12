package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Fever Charm
 * {R}
 * Instant
 * Choose one —
 * • Target creature gains haste until end of turn.
 * • Target creature gets +2/+0 until end of turn.
 * • Fever Charm deals 3 damage to target Wizard creature.
 */
val FeverCharm = card("Fever Charm") {
    manaCost = "{R}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target creature gains haste until end of turn.\n• Target creature gets +2/+0 until end of turn.\n• Fever Charm deals 3 damage to target Wizard creature."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gains haste until end of turn") {
                target = TargetCreature()
                effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0))
            }
            mode("Target creature gets +2/+0 until end of turn") {
                target = TargetCreature()
                effect = Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0))
            }
            mode("Fever Charm deals 3 damage to target Wizard creature") {
                target = TargetCreature(filter = TargetFilter.Creature.withSubtype("Wizard"))
                effect = DealDamageEffect(3, EffectTarget.ContextTarget(0))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "David Martin"
        flavorText = "\"You call that a sneeze?\"\n—Skirk goblin"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/830d1980-f460-4be2-9379-c3f74c8318f3.jpg?1562925918"
    }
}
