package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Crawling Sensation — Shadows over Innistrad #199
 * {2}{G} · Enchantment · Uncommon
 *
 * At the beginning of your upkeep, you may mill two cards.
 * Whenever one or more land cards are put into your graveyard from anywhere for the first time
 * each turn, create a 1/1 green Insect creature token.
 *
 * "For the first time each turn" is turn history, not a cap on the trigger: a land that reached
 * your graveyard earlier in the turn — even while Crawling Sensation wasn't on the battlefield —
 * means no Insect this turn. That's `putIntoYourGraveyard(firstTimeEachTurn = true)`, not
 * `oncePerTurn` (which only counts this ability's own triggers).
 */
val CrawlingSensation = card("Crawling Sensation") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may mill two cards. (You may put the top " +
        "two cards of your library into your graveyard.)\n" +
        "Whenever one or more land cards are put into your graveyard from anywhere for the first " +
        "time each turn, create a 1/1 green Insect creature token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(Patterns.Library.mill(2))
        description = "At the beginning of your upkeep, you may mill two cards."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Land).putIntoYourGraveyard(firstTimeEachTurn = true)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect")
        )
        description = "Whenever one or more land cards are put into your graveyard from anywhere " +
            "for the first time each turn, create a 1/1 green Insect creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f6d5abe-22b5-4ef1-ad74-af3e75e22a07.jpg?1783937733"
        ruling(
            "2016-04-08",
            "If multiple land cards are put into your graveyard at once, Crawling Sensation's last " +
                "ability triggers only once. This could happen because an effect (such as that of " +
                "Crawling Sensation's first ability) put them there from your library at once, or " +
                "because they were destroyed at the same time (such as two land creatures that were " +
                "dealt lethal combat damage)."
        )
    }
}
