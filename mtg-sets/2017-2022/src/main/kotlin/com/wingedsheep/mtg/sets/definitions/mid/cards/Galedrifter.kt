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
 * Galedrifter // Waildrifter (Innistrad: Midnight Hunt #55)
 * {3}{U} · Creature — Hippogriff 3/2 // Creature — Hippogriff Spirit 2/2
 *
 * Front — Galedrifter ({3}{U}, Creature — Hippogriff, 3/2)
 *   Flying
 *   Disturb {4}{U}
 *
 * Back — Waildrifter (Creature — Hippogriff Spirit, 2/2, blue color indicator)
 *   Flying
 *   If Waildrifter would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation: disturb (CR 702.146) is the [disturb] keyword on the front face; the
 * exile-instead clause is a self-only [RedirectZoneChange] carried on the card entity so it
 * applies from every zone, including the stack.
 */
private val GaledrifterFront = card("Galedrifter") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Hippogriff"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "Disturb {4}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    keywords(Keyword.FLYING)
    disturb("{4}{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Daniel Ljunggren"
        flavorText = "\"If you're lost in a storm, follow a gryff. They always know which way calmer waters lie.\"\n—Geor, Havengul fisher"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb34c472-c6ff-4d83-ac8b-a8f279593f98.jpg?1783925646"
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

private val Waildrifter = card("Waildrifter") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Hippogriff Spirit"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "If Waildrifter would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Daniel Ljunggren"
        flavorText = "\"Wait! That's no gryff—at least not anymore.\"\n—Geor, Havengul fisher"
        imageUri = "https://cards.scryfall.io/normal/back/e/b/eb34c472-c6ff-4d83-ac8b-a8f279593f98.jpg?1783925646"
    }
}

val Galedrifter: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GaledrifterFront,
    backFace = Waildrifter,
)
