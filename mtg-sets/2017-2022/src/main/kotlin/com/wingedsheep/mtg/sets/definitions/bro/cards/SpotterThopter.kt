package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Spotter Thopter
 * {8}
 * Artifact Creature — Thopter
 * 4/5
 * Prototype {3}{U} — 2/3
 * Flying
 * When this creature enters, scry X, where X is its power.
 *
 * X is read at resolution ([DynamicAmounts.sourcePower]), falling back to last-known information if the
 * Thopter has left the battlefield — per the 2022-10-14 ruling. A prototyped Thopter scries 2.
 */
val SpotterThopter = card("Spotter Thopter") {
    manaCost = "{8}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Thopter"
    power = 4
    toughness = 5
    oracleText = "Prototype {3}{U} — 2/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Flying\n" +
        "When this creature enters, scry X, where X is its power."

    keywordAbility(KeywordAbility.prototype("{3}{U}", 2, 3))

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(DynamicAmounts.sourcePower())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Michael Phillippi"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d90175ae-4109-4845-b3ff-1531fb67a0e1.jpg?1783920096"
        ruling(
            "2022-10-14",
            "Use Spotter Thopter's power at the time its triggered ability resolves (not the time it triggered) " +
                "to determine the value of X. If Spotter Thopter is no longer on the battlefield at the time the " +
                "ability resolves, use its power as it existed the last time it was on the battlefield."
        )
    }
}
