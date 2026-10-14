package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Spire of Industry — Aether Revolt #184 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add one mana of any color. Activate only if you control an artifact.
 */
val SpireOfIndustry = card("Spire of Industry") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n{T}, Pay 1 life: Add one mana of any color. Activate only if you control an artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.YouControl(GameObjectFilter.Artifact)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "John Avon"
        flavorText = "A beacon of prosperity to some, a shadow of oppression to others."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8331724d-6fab-454a-b06c-b06e499fa552.jpg?1783936719"
    }
}
