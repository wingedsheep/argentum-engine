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
 * Yavimaya Coast
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {G} or {U}. This land deals 1 damage to you.
 */
val YavimayaCoast = card("Yavimaya Coast") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {G} or {U}. This land deals 1 damage to you."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
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
        collectorNumber = "261"
        artist = "Jesper Ejsing"
        flavorText = "As magnigoth trees expanded across the sea into Almaaz and New Argive, it became clear Yavimaya's borders were never confined by its coastline."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ed6c8b0-f154-4678-89d0-9869864ead8d.jpg?1673308410"
    }
}
