package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Dennick, Pious Apprentice // Dennick, Pious Apparition (Innistrad: Midnight Hunt #217)
 * {W}{U} · Legendary Creature — Human Soldier 2/3 // Legendary Creature — Spirit Soldier 3/2
 *
 * Front — Lifelink. Cards in graveyards can't be the targets of spells or abilities.
 *   Disturb {2}{W}{U}
 * Back — Flying. Whenever one or more creature cards are put into graveyards from anywhere,
 *   investigate. This ability triggers only once each turn. If Dennick would be put into a
 *   graveyard from anywhere, exile it instead.
 *
 * Implementation:
 *  - The graveyard lock is [CardsInZonesCantBeTargeted] (default: every card in every graveyard),
 *    read at each targeting site. Disturb casts from a graveyard without targeting, so Dennick
 *    doesn't stop itself (or anyone else's disturb) — only spells and abilities that target.
 *  - The back's trigger is the batch graveyard trigger over every graveyard
 *    (`putIntoGraveyards()`), capped with `oncePerTurn`. Tokens aren't cards, so a dying creature
 *    token doesn't trigger it.
 */
private val DennickPiousApprenticeFront = card("Dennick, Pious Apprentice") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Lifelink\n" +
        "Cards in graveyards can't be the targets of spells or abilities.\n" +
        "Disturb {2}{W}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = CardsInZonesCantBeTargeted()
    }

    disturb("{2}{W}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35cf2d72-931f-47b1-a1b4-916f0383551a.jpg?1783925570"
        ruling(
            "2021-09-24",
            "The second ability of Dennick, Pious Apprentice affects only spells and abilities that " +
                "actually target cards in graveyards. Spells and abilities that affect cards in " +
                "graveyards without targeting them may still affect them, move them to other zones, and so on."
        )
    }
}

private val DennickPiousApparition = card("Dennick, Pious Apparition") {
    manaCost = ""
    colorIdentity = "WU"
    colorIndicator = "WU"
    typeLine = "Legendary Creature — Spirit Soldier"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever one or more creature cards are put into graveyards from anywhere, investigate. " +
        "This ability triggers only once each turn. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")\n" +
        "If Dennick would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).putIntoGraveyards()
        oncePerTurn = true
        effect = Effects.Investigate()
        description = "Whenever one or more creature cards are put into graveyards from anywhere, " +
            "investigate. This ability triggers only once each turn."
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/back/3/5/35cf2d72-931f-47b1-a1b4-916f0383551a.jpg?1783925570"
    }
}

val DennickPiousApprentice: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DennickPiousApprenticeFront,
    backFace = DennickPiousApparition,
)
