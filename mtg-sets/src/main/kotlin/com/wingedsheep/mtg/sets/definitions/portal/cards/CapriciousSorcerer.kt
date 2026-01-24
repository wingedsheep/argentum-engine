package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Capricious Sorcerer
 * {2}{U}
 * Creature - Human Wizard
 * 1/1
 * {T}: Capricious Sorcerer deals 1 damage to any target. Activate only during your turn.
 */
val CapriciousSorcerer = card("Capricious Sorcerer") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        effect = DealDamageEffect(1, EffectTarget.AnyTarget)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8d5eec0-0afe-4af9-ba1b-70282df8cd0a.jpg"
    }
}
