package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Dust Animus
 * {1}{W}
 * Creature — Spirit
 * 2/3
 *
 * Flying
 * If you control five or more untapped lands, this creature enters with two +1/+1 counters and a
 * lifelink counter on it.
 * Plot {1}{W} (You may pay {1}{W} and exile this card from your hand. Cast it as a sorcery on a
 * later turn without paying its mana cost. Plot only as a sorcery.)
 *
 * The conditional enters-with-counters clause is two [EntersWithCounters] replacement effects
 * (CR 121.6 / 614) gated on the same intervening condition — "five or more untapped lands you
 * control" — evaluated as the creature enters. They're independent replacements applied to the
 * same ETB event, so both fire together when the condition holds: two +1/+1 counters and one
 * lifelink keyword counter. `selfOnly` scopes each to this permanent.
 */
val DustAnimus = card("Dust Animus") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "If you control five or more untapped lands, this creature enters with two +1/+1 " +
        "counters and a lifelink counter on it.\n" +
        "Plot {1}{W} (You may pay {1}{W} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 2,
            selfOnly = true,
            condition = Conditions.YouControlAtLeast(5, GameObjectFilter.Land.untapped())
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.LIFELINK),
            count = 1,
            selfOnly = true,
            condition = Conditions.YouControlAtLeast(5, GameObjectFilter.Land.untapped())
        )
    )

    keywordAbility(KeywordAbility.plot("{1}{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70719e7b-6f02-4c1a-9f11-79b2b0d9846a.jpg?1712355260"
    }
}
