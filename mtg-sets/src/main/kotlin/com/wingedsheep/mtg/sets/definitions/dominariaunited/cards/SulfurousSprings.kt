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
 * Sulfurous Springs
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {B} or {R}. This land deals 1 damage to you.
 */
val SulfurousSprings = card("Sulfurous Springs") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {R}. This land deals 1 damage to you."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Bruce Brenneise"
        flavorText = "Everything is flammable in the Burning Isles, even the mana itself."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd5450a-6490-417f-9aea-b6fca6f380d7.jpg?1673308373"
    }
}
