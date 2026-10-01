package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Stinging Hivemaster
 * {2}{B}
 * Creature — Phyrexian Warlock
 * 3/2
 * Toxic 1
 * When this creature dies, create a 1/1 colorless Phyrexian Mite artifact creature token with
 * toxic 1 and "This token can't block."
 */
val StingingHivemaster = card("Stinging Hivemaster") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Warlock"
    power = 3
    toughness = 2
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature dies, create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and \"This token can't block.\""

    keywordAbility(KeywordAbility.toxic(1))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreatePhyrexianMite()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Lie Setiawan"
        flavorText = "From within its form, a single word echoed forth: \"Devour.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdbbcbe1-5317-455e-8d71-a4e2ad0addfc.jpg?1783918039"
    }
}
