package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.encore
import com.wingedsheep.sdk.model.Rarity

/**
 * Impulsive Pilferer — Commander Legends #185
 * {R} · Creature — Goblin Pirate 1/1 · Common
 *
 * When this creature dies, create a Treasure token.
 * Encore {3}{R}
 *
 * Each encore token is a copy of the card, so it carries the dies trigger too: sacrificed at the
 * next end step, every token leaves a Treasure behind.
 */
val ImpulsivePilferer = card("Impulsive Pilferer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pirate"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, create a Treasure token. (It's an artifact with \"{T}, " +
        "Sacrifice this token: Add one mana of any color.\")\n" +
        "Encore {3}{R} ({3}{R}, Exile this card from your graveyard: For each opponent, create a " +
        "token copy that attacks that opponent this turn if able. They gain haste. Sacrifice them " +
        "at the beginning of the next end step. Activate only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateTreasure()
        description = "When this creature dies, create a Treasure token."
    }

    encore("{3}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55ba9bea-5549-45cf-896c-501a1c81fd5a.jpg?1783928813"
    }
}
