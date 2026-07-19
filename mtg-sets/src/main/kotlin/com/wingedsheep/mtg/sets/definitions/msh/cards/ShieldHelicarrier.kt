package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * S.H.I.E.L.D. Helicarrier
 * {4}
 * Artifact — Vehicle — Uncommon (MSH #249)
 * 4/5
 *
 * "Flying"
 * "When this Vehicle enters, create two 1/1 white Soldier creature tokens."
 * "Crew 6"
 */
val ShieldHelicarrier = card("S.H.I.E.L.D. Helicarrier") {
    manaCost = "{4}"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "When this Vehicle enters, create two 1/1 white Soldier creature tokens.\n" +
        "Crew 6 (Tap any number of creatures you control with total power 6 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/e/c/ecd686bf-d14b-491c-b0c5-88fc8f0472f9.jpg?1783902804"
        )
        description = "When this Vehicle enters, create two 1/1 white Soldier creature tokens."
    }

    keywordAbility(KeywordAbility.crew(6))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Arthur Yuan"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/364dc091-3161-4cd7-838c-742b9325fc32.jpg?1783902891"
    }
}
