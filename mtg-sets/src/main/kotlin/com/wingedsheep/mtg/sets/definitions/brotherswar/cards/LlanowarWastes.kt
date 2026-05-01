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
 * Llanowar Wastes
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {B} or {G}. This land deals 1 damage to you.
 */
val LlanowarWastes = card("Llanowar Wastes") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {G}. This land deals 1 damage to you."

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
        effect = Effects.AddMana(Color.GREEN)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Lucas Graciano"
        flavorText = "\"The sylex blast marked not only the end of the war, but the end of Dominaria as most knew it.\"\n—*The Antiquities War*"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10716909-1254-4b2b-997e-23a18994a98d.jpg?1674422216"
    }
}
