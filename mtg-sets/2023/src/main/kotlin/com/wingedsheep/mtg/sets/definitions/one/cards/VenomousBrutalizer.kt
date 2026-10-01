package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Venomous Brutalizer
 * {2}{G}{G}
 * Creature — Phyrexian Knight
 * 4/4
 *
 * Toxic 3 (Players dealt combat damage by this creature also get three poison counters.)
 * When this creature enters, you may pay {1}{G}. If you do, proliferate.
 *
 * The payment is made on resolution ([Effects.MayPay]), so the trigger itself is not optional.
 */
val VenomousBrutalizer = card("Venomous Brutalizer") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Phyrexian Knight"
    power = 4
    toughness = 4
    oracleText = "Toxic 3 (Players dealt combat damage by this creature also get three poison counters.)\n" +
        "When this creature enters, you may pay {1}{G}. If you do, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 3))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(ManaCost.parse("{1}{G}"), Effects.Proliferate())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Mike Jordana"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd9df44a-a0ab-435f-914c-aa11cd88f4ec.jpg?1783918006"
    }
}
