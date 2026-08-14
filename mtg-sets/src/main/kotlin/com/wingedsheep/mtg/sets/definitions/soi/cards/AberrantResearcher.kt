package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aberrant Researcher // Perfected Form (Shadows over Innistrad #49)
 * {3}{U}
 * Creature — Human Insect 3/2 // Creature — Insect Horror 5/4
 *
 * Front — Flying; "At the beginning of your upkeep, mill a card. If an instant or sorcery card was
 *         milled this way, transform this creature."
 * Back  — Flying.
 *
 * One upkeep trigger doing two things in sequence — no player may act between them (printed
 * ruling), which falls out for free: both steps live in a single [Effects.Composite] resolved as one
 * ability. `Patterns.Library.mill(1)` publishes the milled card to the standard `"milled"`
 * collection, and [Conditions.CollectionContainsMatch] reads that collection's card back for the
 * instant-or-sorcery check (the Loafing Giant idiom).
 *
 * Because the check is against the *gathered card*, not against the graveyard, it also satisfies the
 * second ruling: a replacement effect that sends the milled card somewhere other than the graveyard
 * still transforms this creature if that card was an instant or sorcery.
 *
 * The back face has no mana cost, so its blue comes from a color indicator (CR 204).
 */

private val AberrantResearcherFront = card("Aberrant Researcher") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Insect"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, mill a card. If an instant or sorcery card was milled " +
        "this way, transform this creature. (To mill a card, put the top card of your library into " +
        "your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(
            Patterns.Library.mill(1),
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("milled", GameObjectFilter.InstantOrSorcery),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "At the beginning of your upkeep, mill a card. If an instant or sorcery card " +
            "was milled this way, transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Nils Hamm"
        flavorText = "\"Metamorphosis is a process.\"\n—Laboratory notes"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5c9649e-9ae5-4926-bf08-71ba23aa37f1.jpg?1783937809"
        ruling(
            "2016-04-08",
            "No player may take any action between the two steps of Aberrant Researcher's triggered " +
                "ability. If the card put into your graveyard is an instant or sorcery card, Aberrant " +
                "Researcher will have transformed before a player can take any action."
        )
        ruling(
            "2016-04-08",
            "If a replacement effect causes the top card of your library to go to a zone other than " +
                "your graveyard, Aberrant Researcher will still transform if that card was an instant " +
                "or sorcery card."
        )
    }
}

private val PerfectedForm = card("Perfected Form") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Insect Horror"
    power = 5
    toughness = 4
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Nils Hamm"
        flavorText = "The final pages of the experiment log were blank. Investigators found it " +
            "abandoned on a desk in the researcher's lab, open, the pages flipping in the wind from " +
            "a shattered window."
        imageUri = "https://cards.scryfall.io/normal/back/b/5/b5c9649e-9ae5-4926-bf08-71ba23aa37f1.jpg?1783937809"
    }
}

val AberrantResearcher: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AberrantResearcherFront,
    backFace = PerfectedForm,
)
