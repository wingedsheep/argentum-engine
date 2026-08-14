package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect

/**
 * Iron Hills Blacksmith — The Hobbit #16
 * {1}{W} · Creature — Dwarf Artificer · Uncommon
 * 1/1
 *
 * Double strike
 * When this creature enters, create a colorless Equipment artifact token named Axe with
 * "Equipped creature gets +1/+0" and equip {2}.
 *
 * The Axe is a named Equipment with printed abilities, so it goes through
 * [CreatePredefinedTokenEffect] against the `PredefinedTokens.Axe` definition (the Sword /
 * Sturdy Shield shell) rather than an ad-hoc `CreateToken` — that is what gives the token a real
 * card definition, so its static bonus and equip ability actually resolve.
 */
val IronHillsBlacksmith = card("Iron Hills Blacksmith") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    power = 1
    toughness = 1
    oracleText = "Double strike\n" +
        "When this creature enters, create a colorless Equipment artifact token named Axe with " +
        "\"Equipped creature gets +1/+0\" and equip {2}."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreatePredefinedTokenEffect("Axe")
        description = "When this creature enters, create a colorless Equipment artifact token named " +
            "Axe with \"Equipped creature gets +1/+0\" and equip {2}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Jarel Threat"
        flavorText = "Dáin heard Thorin's urgent plea and ordered his smiths to fetch their " +
            "strongest axes and mattocks."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/370e09c2-36c5-4662-8350-1db798afad3e.jpg?1784631771"
    }
}
