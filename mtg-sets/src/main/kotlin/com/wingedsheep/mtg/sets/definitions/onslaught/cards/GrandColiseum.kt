package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Grand Coliseum
 * Land
 * Grand Coliseum enters the battlefield tapped.
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Grand Coliseum deals 1 damage to you.
 */
val GrandColiseum = card("Grand Coliseum") {
    typeLine = "Land"
    oracleText = "Grand Coliseum enters the battlefield tapped.\n{T}: Add {C}.\n{T}: Add one mana of any color. Grand Coliseum deals 1 damage to you."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = CompositeEffect(
            listOf(
                AddAnyColorManaEffect(1),
                DealDamageEffect(1, EffectTarget.Controller, damageSource = EffectTarget.Self)
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "319"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2dc8061-a855-4a81-9eb7-350b355a9b3f.jpg?1562934946"
    }
}
