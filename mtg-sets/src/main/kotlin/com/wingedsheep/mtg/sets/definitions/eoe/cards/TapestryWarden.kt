package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.StationUsingToughness
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tapestry Warden
 * {3}{G}
 * Artifact Creature — Robot Soldier
 * 3/4
 * Vigilance
 * Each creature you control with toughness greater than its power assigns combat damage
 * equal to its toughness rather than its power.
 * Each creature you control with toughness greater than its power stations permanents
 * using its toughness rather than its power.
 */
val TapestryWarden = card("Tapestry Warden") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Robot Soldier"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "Each creature you control with toughness greater than its power assigns combat damage " +
        "equal to its toughness rather than its power.\n" +
        "Each creature you control with toughness greater than its power stations permanents " +
        "using its toughness rather than its power."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.AllCreaturesYouControl,
            onlyWhenToughnessGreaterThanPower = true,
        )
    }

    staticAbility {
        ability = StationUsingToughness
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Andreas Zafiratos"
        flavorText = "Drix never leave their worlds defenseless."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cbbab6c-43ae-4e50-97ce-532a3316591a.jpg?1752947411"
        ruling(
            "2025-07-25",
            "Tapestry Warden's second and third abilities don't actually change any creature's power. They change only the amount of combat damage the creature assigns and how many counters are put on a permanent with station when the creature is tapped to pay the cost of a station ability. All other rules and effects that check power or toughness use the real values, even if they cause damage \"equal to a creature's power\" to be dealt."
        )
        ruling(
            "2025-07-25",
            "If a station ability you control resolves while you control Tapestry Warden, but the creature tapped to pay the cost of that station ability is no longer on the battlefield, check the characteristics of that creature as it last existed on the battlefield. If its toughness was greater than its power, use its toughness to determine how many counters are put on the permanent with station."
        )
        ruling(
            "2025-07-25",
            "If you activate a station ability while you control Tapestry Warden, but you no longer control Tapestry Warden at the time that ability resolves, use the power of the creature tapped to pay the cost of the station ability to determine how many counters are put on the permanent with station."
        )
    }
}
