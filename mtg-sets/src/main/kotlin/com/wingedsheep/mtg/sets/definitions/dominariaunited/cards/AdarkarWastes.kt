package com.wingedsheep.mtg.sets.definitions.dominariaunited.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adarkar Wastes
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {W} or {U}. This land deals 1 damage to you.
 */
val AdarkarWastes = card("Adarkar Wastes") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {W} or {U}. This land deals 1 damage to you."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Piotr Dura"
        flavorText = "Left barren by the Brothers' War, it stands as a stark reminder of Dominaria's likely fate should the Coalition fail."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08ae1037-6f70-41a9-b75e-98fa9a2152c8.jpg?1673308279"
    }
}
