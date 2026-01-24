package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Soul Shred
 * {3}{B}{B}
 * Sorcery
 * Soul Shred deals 3 damage to target nonblack creature. You gain 3 life.
 */
val SoulShred = card("Soul Shred") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature(filter = CreatureTargetFilter.NotColor(Color.BLACK))
        effect = DealDamageEffect(3, EffectTarget.ContextTarget(0)) then
                GainLifeEffect(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Brom"
        flavorText = "Life flows from one to another."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29a0b1c2-3d4e-5f6a-7b8c-9d0e1f2a3b4c.jpg"
    }
}
