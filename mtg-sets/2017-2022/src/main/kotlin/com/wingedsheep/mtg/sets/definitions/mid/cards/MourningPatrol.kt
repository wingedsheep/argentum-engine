package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Mourning Patrol // Morning Apparition (Innistrad: Midnight Hunt #28)
 * {2}{W} · Creature — Human Soldier 2/3 // Creature — Spirit Soldier 2/1
 *
 * Front — Mourning Patrol ({2}{W}, Creature — Human Soldier, 2/3)
 *   Vigilance
 *   Disturb {3}{W}
 *
 * Back — Morning Apparition (Creature — Spirit Soldier, 2/1, white color indicator)
 *   Flying, vigilance
 *   If Morning Apparition would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation: disturb (CR 702.146) is the [disturb] keyword on the front face; the
 * exile-instead clause is a self-only [RedirectZoneChange] carried on the card entity so it
 * applies from every zone, including the stack.
 */
private val MourningPatrolFront = card("Mourning Patrol") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "Disturb {3}{W} (You may cast this card from your graveyard transformed for its disturb cost.)"

    keywords(Keyword.VIGILANCE)
    disturb("{3}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Greg Opalinski"
        flavorText = "\"It's creepy out here, isn't it, boy? I always feel like someone's watching us.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d3687e2-09e0-4753-aa02-88a19bde3330.jpg?1783925661"
        ruling(
            "2021-09-24",
            "When you cast a spell using a card's disturb ability, the card is put onto the stack " +
                "with its back face up. The resulting spell has all the characteristics of that face."
        )
        ruling(
            "2021-09-24",
            "The mana value of a spell cast using disturb is determined by the mana cost on the " +
                "front face of the card, no matter what the total cost to cast the spell was."
        )
        ruling(
            "2021-09-24",
            "The back face of each card with disturb has an ability that instructs its controller " +
                "to exile it if it would be put into a graveyard from anywhere. This includes going " +
                "to the graveyard from the stack, so if the spell is countered after you cast it " +
                "using the disturb ability, it will be put into exile."
        )
    }
}

private val MorningApparition = card("Morning Apparition") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Spirit Soldier"
    power = 2
    toughness = 1
    oracleText = "Flying, vigilance\n" +
        "If Morning Apparition would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Greg Opalinski"
        flavorText = "\"You could see the spirits all along, couldn't you? Well, now I can too.\""
        imageUri = "https://cards.scryfall.io/normal/back/2/d/2d3687e2-09e0-4753-aa02-88a19bde3330.jpg?1783925661"
    }
}

val MourningPatrol: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = MourningPatrolFront,
    backFace = MorningApparition,
)
