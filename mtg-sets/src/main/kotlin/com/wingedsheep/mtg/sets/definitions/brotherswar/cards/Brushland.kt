package com.wingedsheep.mtg.sets.definitions.brotherswar.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brushland
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {G} or {W}. This land deals 1 damage to you.
 */
val Brushland = card("Brushland") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {G} or {W}. This land deals 1 damage to you."

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
        effect = Effects.AddMana(Color.WHITE)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Thomas Stoop"
        flavorText = "\"Urza fortified every thirty miles on the path to Tomakul, creating a network of trenches the brothers would trade for decades.\"\n—*The Antiquities War*"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18d236ce-3b78-403a-b5f9-4fb44123d85b.jpg?1674422171"
    }
}
