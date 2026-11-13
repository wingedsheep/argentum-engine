package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kor Castigator
 * {1}{W}
 * Creature — Kor Wizard Ally
 * 3/1
 * This creature can't be blocked by Eldrazi Scions.
 *
 * "Eldrazi Scions" names two creature types at once, so the blocker filter requires both the
 * Eldrazi and the Scion subtype (chained `withSubtype` predicates are ANDed).
 */
val KorCastigator = card("Kor Castigator") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Wizard Ally"
    oracleText = "This creature can't be blocked by Eldrazi Scions."
    power = 3
    toughness = 1

    staticAbility {
        ability = CantBeBlockedBy(
            GameObjectFilter.Creature.withSubtype(Subtype.ELDRAZI).withSubtype("Scion")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Greg Opalinski"
        flavorText = "\"He told us not to worry, and he walked into the cavern. The swirling mass of " +
            "Eldrazi parted to admit him, then closed in his wake.\"\n—Akiri, kor line-slinger"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4db2f0e-d7d4-417b-9b94-5ade727907e9.jpg"
    }
}
