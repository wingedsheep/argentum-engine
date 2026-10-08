package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Niv-Mizzet, Guildpact — Murders at Karlov Manor #220
 * {W}{U}{B}{R}{G} · Legendary Creature — Dragon Avatar · 6/6 · Rare
 *
 * X is the number of *different color pairs* among permanents the controller controls that are
 * exactly two colors (CR 105.2c) — ten pairs exist, so X is bounded by 10, and two Boros
 * permanents count once. Mono-colored, three-or-more-colored, and colorless permanents contribute
 * nothing; Niv-Mizzet itself is five colors and so never counts toward its own trigger.
 *
 * Per the 2024-02-02 ruling X is checked once, as the ability resolves. The three sub-effects each
 * read the amount, which is the same value: state-based actions don't run mid-resolution, so
 * nothing the damage does can remove a permanent from the count before the draw and the life gain
 * read it.
 */
val NivMizzetGuildpact = card("Niv-Mizzet, Guildpact") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon Avatar"
    oracleText = "Flying, hexproof from multicolored\n" +
        "Whenever Niv-Mizzet deals combat damage to a player, it deals X damage to any target, " +
        "target player draws X cards, and you gain X life, where X is the number of different " +
        "color pairs among permanents you control that are exactly two colors."
    power = 6
    toughness = 6

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.Multicolored))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val damaged = target(Targets.Any)
        val drawer = target(Targets.Player)
        val colorPairs = DynamicAmounts.colorPairsAmongPermanents()
        effect = Effects.DealDamage(colorPairs, damaged) then
            Effects.DrawCards(colorPairs, drawer) then
            Effects.GainLife(colorPairs)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32a8fda6-8614-45cd-879c-0cb7fa29647e.jpg?1783912843"
        ruling(
            "2024-02-02",
            "A \"color pair\" is exactly two colors. There are ten color pairs in Magic: " +
                "white-blue, white-black, blue-black, blue-red, black-red, black-green, red-green, " +
                "red-white, green-white, and green-blue.",
        )
        ruling("2024-02-02", "The value of X is checked only once, as Niv-Mizzet, Guildpact's triggered ability resolves.")
    }
}
