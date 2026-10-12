package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val TalismanOfConviction = card("Talisman of Conviction") {
    manaCost = "{2}"
    colorIdentity = "RW"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n{T}: Add {R} or {W}. This artifact deals 1 damage to you."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Lindsey Look"
        flavorText = "\"The sun banishes darkness every morning. I too will rise with such assuredness.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71148fd3-0c2c-459e-b8f5-735a0a8dd87f.jpg?1783933073"
    }
}
