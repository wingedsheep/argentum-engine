package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Pillar of Origins — Ixalan #241
 * {2} Artifact
 * As this artifact enters, choose a creature type.
 * {T}: Add one mana of any color. Spend this mana only to cast a creature spell of the chosen type.
 *
 * The artifact twin of Unclaimed Territory: `creatureOnly = true` limits the restricted mana to
 * creature spells of the chosen type.
 */
val PillarOfOrigins = card("Pillar of Origins") {
    manaCost = "{2}"
    typeLine = "Artifact"
    colorIdentity = ""
    oracleText =
        "As this artifact enters, choose a creature type.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a creature spell of the chosen type."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorManaSpendOnChosenType(creatureOnly = true)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Dimitar Marinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f888facd-e462-4794-a774-d765d1a1edfd.jpg?1783935705"
        ruling(
            "2018-01-19",
            "To choose a creature type, you must choose an existing creature type, such as Vampire or Knight. You can't choose multiple creature types, such as \"Vampire Knight.\" Card types such as artifact can't be chosen, nor can subtypes that aren't creature types, such as Jace, Vehicle, or Treasure."
        )
    }
}
