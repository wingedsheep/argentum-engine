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
 * Caves of Koilos
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {W} or {B}. This land deals 1 damage to you.
 */
val CavesOfKoilos = card("Caves of Koilos") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {W} or {B}. This land deals 1 damage to you."

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
        effect = Effects.AddMana(Color.BLACK)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Julian Kok Joon Wen"
        flavorText = "After years of silence, the caves once again echo with the grisly sounds of Phyrexians preparing for war."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99264beb-2149-4cd4-9880-f0dc5c570c1b.jpg?1673308286"
    }
}
