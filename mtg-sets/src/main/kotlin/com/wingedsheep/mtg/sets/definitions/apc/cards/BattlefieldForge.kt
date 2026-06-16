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
 * Battlefield Forge
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {R} or {W}. This land deals 1 damage to you.
 */
val BattlefieldForge = card("Battlefield Forge") {
    typeLine = "Land"
    colorIdentity = "WR"
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
        collectorNumber = "139"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9c25e71-0140-48fe-8b9e-33b4b50c5c12.jpg"
    }
}
