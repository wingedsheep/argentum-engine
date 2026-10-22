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
 * Beloved Beggar // Generous Soul (Innistrad: Midnight Hunt #3)
 * {1}{W} · Creature — Human Peasant 0/4 // Creature — Spirit 4/4
 *
 * Front — Beloved Beggar ({1}{W}, Creature — Human Peasant, 0/4)
 *   Disturb {4}{W}{W}
 *
 * Back — Generous Soul (Creature — Spirit, 4/4, white color indicator)
 *   Flying, vigilance
 *   If Generous Soul would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation: disturb (CR 702.146) is the [disturb] keyword on the front face; the
 * exile-instead clause is a self-only [RedirectZoneChange] carried on the card entity so it
 * applies from every zone, including the stack.
 */
private val BelovedBeggarFront = card("Beloved Beggar") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Peasant"
    power = 0
    toughness = 4
    oracleText = "Disturb {4}{W}{W} (You may cast this card from your graveyard transformed for its disturb cost.)"

    disturb("{4}{W}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Francisco Miyara"
        flavorText = "Charity is rare on Innistrad, but kindness is always repaid."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6109b54e-56c5-4014-9f6d-d5f7a0fd725d.jpg?1783925674"
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

private val GenerousSoul = card("Generous Soul") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "If Generous Soul would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Francisco Miyara"
        flavorText = "As long as evil threatens his town, the Blessed Sleep can wait."
        imageUri = "https://cards.scryfall.io/normal/back/6/1/6109b54e-56c5-4014-9f6d-d5f7a0fd725d.jpg?1783925674"
    }
}

val BelovedBeggar: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BelovedBeggarFront,
    backFace = GenerousSoul,
)
