package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hermes, Overseer of Elpis — Final Fantasy Commander #36 (canonical printing)
 * {3}{U} · Legendary Creature — Elder Wizard · 2/4
 *
 * Whenever you cast a noncreature spell, create a 1/1 blue Bird creature token with flying and
 * vigilance.
 * Whenever you attack with one or more Birds, scry 2.
 *
 * The attack trigger is `Triggers.you.attacks(with)` — one firing per declare-attackers step, not
 * one per Bird.
 */
val HermesOverseerOfElpis = card("Hermes, Overseer of Elpis") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elder Wizard"
    power = 2
    toughness = 4
    oracleText = "Whenever you cast a noncreature spell, create a 1/1 blue Bird creature token with " +
        "flying and vigilance.\n" +
        "Whenever you attack with one or more Birds, scry 2."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/0/2/023cebe4-ebe4-4398-9fd7-d3507660040a.jpg?1783906141",
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(GameObjectFilter.Creature.withSubtype("Bird"))
        effect = Patterns.Library.scry(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Darius Zablockis"
        flavorText = "\"For us, there may be no higher purpose than to live for our world, but what " +
            "of the other living beings out there?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e04a05e5-7bff-46a4-aae1-6713977e6e30.jpg?1783906363"
        ruling("2025-06-06", "Hermes's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
