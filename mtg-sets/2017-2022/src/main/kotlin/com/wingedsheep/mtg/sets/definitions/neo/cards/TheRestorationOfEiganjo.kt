package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Restoration of Eiganjo // Architect of Restoration
 * {2}{W} — Enchantment — Saga // Enchantment Creature — Fox Monk 3/4
 *
 * Front — The Restoration of Eiganjo:
 *   I — Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle.
 *   II — You may discard a card. When you do, return target permanent card with mana value 2 or
 *        less from your graveyard to the battlefield tapped.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Architect of Restoration:
 *   Vigilance
 *   Whenever this creature attacks or blocks, create a 1/1 colorless Spirit creature token.
 *
 * Chapter II is a reflexive trigger: the target is chosen only after the discard, so the card just
 * discarded is a legal choice. "Attacks or blocks" is two triggers, as on Howling Golem.
 */
private fun spiritToken() = Effects.CreateToken(
    power = 1,
    toughness = 1,
    creatureTypes = setOf("Spirit"),
)

private val ArchitectOfRestoration = card("Architect of Restoration") {
    manaCost = ""
    colorIndicator = "W"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Fox Monk"
    oracleText = "Vigilance\nWhenever this creature attacks or blocks, create a 1/1 colorless Spirit creature token."
    power = 3
    toughness = 4

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = spiritToken()
        description = "Whenever this creature attacks, create a 1/1 colorless Spirit creature token."
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = spiritToken()
        description = "Whenever this creature blocks, create a 1/1 colorless Spirit creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Titus Lunter"
        flavorText = "\"A visionary looks at rubble and sees not only what was, but also what could be.\"\n" +
            "—Onyx-Eye, architect of Eiganjo"
        imageUri = "https://cards.scryfall.io/normal/back/a/1/a11a33ae-e7fa-4bd4-8cd8-3a3239a29bcc.jpg?1783923920"
    }
}

private val TheRestorationOfEiganjoFront = card("The Restoration of Eiganjo") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle.\n" +
        "II — You may discard a card. When you do, return target permanent card with mana value 2 or " +
        "less from your graveyard to the battlefield tapped.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    sagaChapter(2) {
        effect = Effects.ReflexiveTrigger(
            action = Effects.Discard(1),
            optional = true
        ) {
            val permanentCard = target(
                TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
                    .manaValueAtMost(2)
            )
            effect = Effects.PutOntoBattlefieldFromGraveyard(permanentCard, tapped = true)
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a11a33ae-e7fa-4bd4-8cd8-3a3239a29bcc.jpg?1783923920"
    }
}

val TheRestorationOfEiganjo: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheRestorationOfEiganjoFront,
    backFace = ArchitectOfRestoration,
)
