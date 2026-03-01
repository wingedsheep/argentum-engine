package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Winterflame
 * {1}{U}{R}
 * Instant
 * Choose one or both —
 * • Tap target creature.
 * • Winterflame deals 2 damage to target creature.
 */
val Winterflame = card("Winterflame") {
    manaCost = "{1}{U}{R}"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n• Tap target creature.\n• Winterflame deals 2 damage to target creature."

    spell {
        // Modeled as 3 modes: tap only, damage only, or both (choose one or both)
        modal(chooseCount = 1) {
            mode("Tap target creature") {
                val t = target("creature", TargetCreature())
                effect = Effects.Tap(t)
            }
            mode("Winterflame deals 2 damage to target creature") {
                val t = target("creature", TargetCreature())
                effect = DealDamageEffect(2, t)
            }
            mode("Tap target creature and deal 2 damage to target creature") {
                val tapTarget = target("creature to tap", TargetCreature())
                val damageTarget = target("creature to damage", TargetCreature())
                effect = CompositeEffect(listOf(
                    Effects.Tap(tapTarget),
                    DealDamageEffect(2, damageTarget)
                ))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Richard Wright"
        flavorText = "\"The mountains scream with the dragons' throats.\"\n—Chianul, Who Whispers Twice"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8924ab9-fa55-4348-b67d-b2b9e48a357a.jpg?1562792511"
    }
}
