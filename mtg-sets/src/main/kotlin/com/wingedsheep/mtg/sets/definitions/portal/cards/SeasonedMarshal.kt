package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.triggers.OnAttack
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Seasoned Marshal
 * {2}{W}{W}
 * Creature - Human Soldier
 * 2/2
 * Whenever Seasoned Marshal attacks, you may tap target creature.
 */
val SeasonedMarshal = card("Seasoned Marshal") {
    manaCost = "{2}{W}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = OnAttack()
        optional = true
        target = TargetCreature()
        effect = TapUntapEffect(EffectTarget.ContextTarget(0), tap = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17db0060-3667-4c8c-ae9b-d62dceac64e3.jpg"
    }
}
