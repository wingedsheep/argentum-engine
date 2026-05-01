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
 * Underground River
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {U} or {B}. This land deals 1 damage to you.
 */
val UndergroundRiver = card("Underground River") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {U} or {B}. This land deals 1 damage to you."

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
        effect = Effects.AddMana(Color.BLACK)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Volkan Baǵa"
        flavorText = "\"The war polluted the land, turning what had once been fruitful fields into fetid morasses of mud and rusted corpses.\"\n—*The Antiquities War*"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb52820c-8660-4c4a-bb64-5b2fc580b6a3.jpg?1674422238"
    }
}
