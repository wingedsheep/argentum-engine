package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Eclipsed Realms
 * Land
 *
 * As this land enters, choose Elemental, Elf, Faerie, Giant, Goblin, Kithkin, Merfolk, or Treefolk.
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a spell of the chosen type
 * or activate an ability of a source of the chosen type.
 *
 * Ruling: Sources of the chosen creature type include any objects with the chosen subtype.
 * For example, if you chose Kithkin, you could spend the mana to activate the ability of a
 * Kithkin permanent you control or a Kithkin card in your hand or graveyard.
 */
val EclipsedRealms = card("Eclipsed Realms") {
    typeLine = "Land"
    oracleText =
        "As Eclipsed Realms enters, choose Elemental, Elf, Faerie, Giant, Goblin, Kithkin, Merfolk, or Treefolk.\n" +
        "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a spell of the chosen type " +
        "or activate an ability of a source of the chosen type."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CREATURE_TYPE,
            allowedCreatureTypes = listOf(
                "Elemental", "Elf", "Faerie", "Giant",
                "Goblin", "Kithkin", "Merfolk", "Treefolk"
            )
        )
    )

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
        collectorNumber = "263"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a174f0db-8b4f-4c37-9583-44c92d37b9c0.jpg?1767862754"
    }
}
