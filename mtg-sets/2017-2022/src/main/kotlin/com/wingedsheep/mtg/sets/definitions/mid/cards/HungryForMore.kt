package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Hungry for More
 * {B}{R}
 * Sorcery
 * Create a 3/1 black and red Vampire creature token with trample, lifelink, and haste. Sacrifice
 * it at the beginning of the next end step.
 * Flashback {1}{B}{R}
 *
 * `sacrificeAtStep = Step.END` schedules the delayed sacrifice for exactly the created token.
 */
val HungryForMore = card("Hungry for More") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Create a 3/1 black and red Vampire creature token with trample, lifelink, and haste. " +
        "Sacrifice it at the beginning of the next end step.\n" +
        "Flashback {1}{B}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.RED),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.TRAMPLE, Keyword.LIFELINK, Keyword.HASTE),
            sacrificeAtStep = Step.END,
            imageUri = "https://cards.scryfall.io/normal/front/0/f/0f8fe08d-b469-4471-8a7d-cf75850ba312.jpg?1783925221",
        )
    }

    keywordAbility(KeywordAbility.flashback("{1}{B}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce09c0df-d022-4ebf-8f7f-8a913249e75f.jpg?1783925560"
    }
}
