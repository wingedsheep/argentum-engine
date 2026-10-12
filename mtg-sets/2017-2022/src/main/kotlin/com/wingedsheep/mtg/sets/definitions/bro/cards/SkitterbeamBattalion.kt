package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Skitterbeam Battalion
 * {9}
 * Artifact Creature — Construct
 * 4/4
 * Prototype {3}{R}{R} — 2/2
 * Trample, haste
 * When this creature enters, if you cast it, create two tokens that are copies of it.
 *
 * "If you cast it" is a genuine intervening-if (CR 603.4) on [Conditions.WasCast] — from any zone,
 * so the token copies (which weren't cast) never re-trigger it. The copies take the permanent's
 * copiable values, which for a prototyped Battalion are its prototype mana cost, color and 2/2 size
 * (ruling): the prototyped permanent's `CardComponent` carries those, and
 * [Effects.CreateTokenCopyOfSelf] copies that component.
 */
val SkitterbeamBattalion = card("Skitterbeam Battalion") {
    manaCost = "{9}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Prototype {3}{R}{R} — 2/2 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Trample, haste\n" +
        "When this creature enters, if you cast it, create two tokens that are copies of it."

    keywordAbility(KeywordAbility.prototype("{3}{R}{R}", 2, 2))

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Effects.CreateTokenCopyOfSelf(count = 2)
        description = "When this creature enters, if you cast it, create two tokens that are copies of it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "165"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38fc1715-4ddb-4404-83c6-0b8a61922578.jpg?1783920054"
        ruling(
            "2022-10-14",
            "The copies will have the same mana cost, mana value, color, power, and toughness as Skitterbeam " +
                "Battalion, which vary depending on whether it was cast as a prototyped spell."
        )
    }
}
