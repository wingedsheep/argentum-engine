package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Palladia-Mors, the Ruiner — Core Set 2019 #219
 * {3}{R}{G}{W} · Legendary Creature — Elder Dragon · 6/6
 *
 * Flying, vigilance, trample
 * Palladia-Mors has hexproof as long as it hasn't dealt damage yet.
 *
 * Same shape as Karakyk Guardian: a [ConditionalStaticAbility] granting itself hexproof, gated on
 * `Not(SourceHasDealtDamage)`. The dealt-damage record is a battlefield component, so a new object
 * (after a zone change) has hexproof again, per the ruling.
 */
val PalladiaMorsTheRuiner = card("Palladia-Mors, the Ruiner") {
    manaCost = "{3}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying, vigilance, trample\n" +
        "Palladia-Mors has hexproof as long as it hasn't dealt damage yet."

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.TRAMPLE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, Filters.Self),
            condition = Conditions.Not(Conditions.SourceHasDealtDamage)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Svetlin Velinov"
        flavorText = "Sister of Nicol Bolas. Survivor of the Elder Dragon War. The most vicious of her kin."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4663bd13-243e-4b8c-9ca8-158576b58803.jpg?1783934520"
        ruling(
            "2018-07-13",
            "If Palladia-Mors leaves the battlefield and returns, it's considered a new object, even if it's " +
                "represented by the same card. It has hexproof again until it deals damage."
        )
        ruling(
            "2018-07-13",
            "If a triggered ability triggers at the same time that Paladia-Mors deals damage for the first time, " +
                "targets are chosen for that ability after Palladia-Mors no longer has hexproof."
        )
    }
}
