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
 * Shivan Reef
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {U} or {R}. This land deals 1 damage to you.
 */
val ShivanReef = card("Shivan Reef") {
    typeLine = "Land"
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
        collectorNumber = "255"
        artist = "Andrew Mar"
        flavorText = "The boiling seas around Shiv provided a welcome layer of protection for the reclaimed Mana Rig."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a338107b-0960-4496-a9a5-f7b672e7c043.jpg?1673308366"
    }
}
