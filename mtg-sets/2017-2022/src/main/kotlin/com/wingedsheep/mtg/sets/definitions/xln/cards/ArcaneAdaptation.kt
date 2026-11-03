package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Arcane Adaptation (XLN #46)
 * {2}{U}  Enchantment
 *
 * As this enchantment enters, choose a creature type.
 * Creatures you control are the chosen type in addition to their other types. The same is
 * true for creature spells you control and creature cards you own that aren't on the
 * battlefield.
 *
 * Same static as Leyline of Transformation: [EntersWithChoice] locks in the creature type,
 * [GrantChosenSubtype] adds it to creatures you control (Layer 4), and the two cross-zone
 * flags extend it to creature spells you control and creature cards you own off the
 * battlefield.
 */
val ArcaneAdaptation = card("Arcane Adaptation") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Creatures you control are the chosen type in addition to their other types. The same is true for creature spells you control and creature cards you own that aren't on the battlefield."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = GrantChosenSubtype(
            filter = GroupFilter.AllCreaturesYouControl,
            includeControlledSpells = true,
            includeOwnedCardsOutsideBattlefield = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf3edaaf-cf63-4e17-94ae-9d9991d9fb5f.jpg?1783935787"
        ruling(
            "2017-09-29",
            "Replacement effects that modify creatures of a certain type as they enter the battlefield will apply after you apply Arcane Adaptation’s effect. This is a change from previous rules. If you control Arcane Adaptation and the Aether Revolt card Metallic Mimic, with the same creature type chosen for both, then any creature you control will enter the battlefield with an additional +1/+1 counter on it."
        )
    }
}
