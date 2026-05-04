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
 * Battlefield Forge
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {R} or {W}. This land deals 1 damage to you.
 */
val BattlefieldForge = card("Battlefield Forge") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {R} or {W}. This land deals 1 damage to you."

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
        effect = Effects.AddMana(Color.WHITE)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Thomas Stoop"
        flavorText = "\"As Terisiare's mines ran dry, scrap metal became increasingly plentiful.\"\n—*The Antiquities War*"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642584bb-7586-4796-9b94-f01ec5bd9e9f.jpg?1674422149"
    }
}
