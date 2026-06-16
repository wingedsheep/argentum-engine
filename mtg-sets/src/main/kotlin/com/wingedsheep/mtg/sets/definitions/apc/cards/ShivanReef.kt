package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shivan Reef
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {U} or {R}. This land deals 1 damage to you.
 */
val ShivanReef = card("Shivan Reef") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "{T}: Add {C}.\n{T}: Add {U} or {R}. This land deals 1 damage to you."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
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

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3403143-2b4e-4408-b138-c856bbc1e9a5.jpg"
    }
}
