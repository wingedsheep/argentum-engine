package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Grond, the Gatebreaker
 * {3}{B}
 * Legendary Artifact — Vehicle
 * 5/5
 * Trample
 * As long as it's your turn and you control an Army, Grond is an artifact creature.
 * Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle
 *   becomes an artifact creature until end of turn.)
 *
 * Vehicle = artifact subtype (CR 301.7); a Vehicle has printed power/toughness but isn't a
 * creature unless an effect makes it one. Crew (CR 702.122) is a built-in keyword ability that
 * taps creatures totalling power N to animate the Vehicle to its printed P/T (with its keywords,
 * here Trample) until end of turn. The static is a conditional Layer-4 type change: while it's
 * your turn AND you control an Army, Grond gains the CREATURE type and so uses its printed 5/5.
 */
val GrondTheGatebreaker = card("Grond, the Gatebreaker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "As long as it's your turn and you control an Army, Grond is an artifact creature.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.TRAMPLE)

    // As long as it's your turn and you control an Army, Grond is an artifact creature.
    staticAbility {
        condition = Conditions.All(
            Conditions.IsYourTurn,
            Conditions.YouControl(GameObjectFilter.Creature.youControl().withSubtype("Army"))
        )
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // Crew 3
    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 3))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Ramazan Kazaliev"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bc61b28-afdd-4de9-829b-ffe5ca7c7f19.jpg?1738052978"
    }
}
