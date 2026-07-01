package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Adaptive Automaton
 * {3}
 * Artifact Creature — Construct
 * 2/2
 *
 * As this creature enters, choose a creature type.
 * This creature is the chosen type in addition to its other types.
 * Other creatures you control of the chosen type get +1/+1.
 *
 * The chosen creature type is captured by [EntersWithChoice]. [GrantChosenSubtype] (default
 * filter: the source itself) makes this creature the chosen type in addition to Construct, and
 * the lord [ModifyStats] buffs the *other* creatures you control of that chosen type
 * (`ChosenSubtypeCreatures(excludeSelf = true)`).
 */
val AdaptiveAutomaton = card("Adaptive Automaton") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "As this creature enters, choose a creature type.\n" +
        "This creature is the chosen type in addition to its other types.\n" +
        "Other creatures you control of the chosen type get +1/+1."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    // This creature is the chosen type in addition to its other types.
    staticAbility {
        ability = GrantChosenSubtype()
    }

    // Other creatures you control of the chosen type get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures(excludeSelf = true).youControl()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Igor Kieryluk"
        flavorText = "Such loyalty can only be made."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79e42ead-df6e-4181-ae2b-a2abfc3f1d7c.jpg?1782714857"
    }
}
