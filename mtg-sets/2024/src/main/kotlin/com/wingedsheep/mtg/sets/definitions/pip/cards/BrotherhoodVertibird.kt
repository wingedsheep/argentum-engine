package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Brotherhood Vertibird
 * {3}
 * Artifact — Vehicle
 * * /4
 *
 * Flying
 * Brotherhood Vertibird's power is equal to the number of artifacts you control.
 * Crew 2
 *
 * Only power is dynamic (printed toughness stays 4), so the single-stat `dynamicPower(...)`
 * characteristic-defining ability applies in Layer 7b the way Lumbering Worldwagon's does, counting
 * artifacts you control with Broodstar's aggregate. On the battlefield the Vertibird is itself an
 * artifact and so counts itself (ruling).
 */
val BrotherhoodVertibird = card("Brotherhood Vertibird") {
    manaCost = "{3}"
    typeLine = "Artifact — Vehicle"
    toughness = 4
    oracleText = "Flying\n" +
        "Brotherhood Vertibird's power is equal to the number of artifacts you control.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or greater: " +
        "This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    dynamicPower(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count())

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Daarken"
        flavorText = "As tough as the armored troops it carries."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c207c76d-5f10-4fd9-9826-cf0611bfa2d0.jpg?1783912380"
        ruling(
            "2024-03-08",
            "The ability that defines Brotherhood Vertibird's power works in all zones, not just the " +
                "battlefield. As long as Brotherhood Vertibird is on the battlefield (and still an " +
                "artifact), that ability will count Brotherhood Vertibird itself."
        )
    }
}
