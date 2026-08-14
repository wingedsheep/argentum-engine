package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Tuinvale Guide
 * {3}{W}
 * Creature — Faerie Scout
 * 2/3
 *
 * Flying
 * Celebration — This creature gets +1/+0 and has lifelink as long as two or more nonland
 * permanents entered the battlefield under your control this turn.
 *
 * The static half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning),
 * combining what [ArmoryMice] and [GallantPieWielder] each do on their own: one printed ability
 * that touches layer 7c (the +1/+0) *and* layer 6 (lifelink), so it's a [CompositeStaticAbility]
 * rather than two independent statics. Removing the ability removes both halves together, and the
 * shared [Conditions.Celebration] gate is evaluated once per projection — so the bonus and the
 * keyword appear and vanish in lockstep with the turn's second nonland permanent.
 *
 * Per the WOE rulings, Celebration is a pure past-events check: the two permanents don't have to
 * still be on the battlefield or still be yours, and a third one adds nothing.
 */
val TuinvaleGuide = card("Tuinvale Guide") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Faerie Scout"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Celebration — This creature gets +1/+0 and has lifelink as long as two or more nonland " +
        "permanents entered the battlefield under your control this turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = Filters.Self),
                    GrantKeyword(Keyword.LIFELINK, Filters.Self),
                )
            ),
            condition = Conditions.Celebration,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Weary traveler, follow me. The night is a cage, and my light is the key.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01334fed-781e-4864-83fd-d37f787a778b.jpg?1783915125"

        ruling(
            "2023-09-01",
            "The permanents that entered the battlefield don't need to remain on the battlefield or " +
                "under your control. Celebration abilities are checking for past events, not the " +
                "current game state."
        )
    }
}
