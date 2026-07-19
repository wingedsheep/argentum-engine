package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Secluded Courtyard
 * Land
 * As this land enters, choose a creature type.
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a creature spell of the chosen
 * type or activate an ability of a creature source of the chosen type.
 *
 * Unclaimed Territory shape (no uncounterable rider), so the restricted mana also covers
 * activated abilities of sources of the chosen type — `creatureOnly = false` (the facade default).
 */
val SecludedCourtyard = card("Secluded Courtyard") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText =
        "As this land enters, choose a creature type.\n" +
        "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a creature spell of the chosen type or activate an ability of a creature source of the chosen type."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorManaSpendOnChosenType()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "275"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0539b1a5-8704-476f-ba1f-2fe01190e157.jpg?1783923815"
    }
}
