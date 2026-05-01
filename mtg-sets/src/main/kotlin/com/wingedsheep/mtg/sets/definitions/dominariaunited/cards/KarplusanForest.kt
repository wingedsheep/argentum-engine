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
 * Karplusan Forest
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {R} or {G}. This land deals 1 damage to you.
 */
val KarplusanForest = card("Karplusan Forest") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {R} or {G}. This land deals 1 damage to you."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
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

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Sam Burley"
        flavorText = "Between jagged, snowcapped peaks, the dense bands of hardy evergreens provide the perfect cover for roving bands of orc and goblin raiders."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b89b2c79-e3d3-4ef9-bfc8-f9c090975011.jpg?1673308330"
    }
}
