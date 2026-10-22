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
 * Baithook Angler // Hook-Haunt Drifter (Innistrad: Midnight Hunt #42)
 * {1}{U} · Creature — Human Peasant 2/1 // Creature — Spirit 1/2
 *
 * Front — Baithook Angler ({1}{U}, Creature — Human Peasant, 2/1)
 *   Disturb {1}{U}
 *
 * Back — Hook-Haunt Drifter (Creature — Spirit, 1/2, blue color indicator)
 *   Flying
 *   If Hook-Haunt Drifter would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation: disturb (CR 702.146) is the [disturb] keyword on the front face; the
 * exile-instead clause is a self-only [RedirectZoneChange] carried on the card entity so it
 * applies from every zone, including the stack.
 */
private val BaithookAnglerFront = card("Baithook Angler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 1
    oracleText = "Disturb {1}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    disturb("{1}{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Uriah Voth"
        flavorText = "Geor was determined to stay out until he got a bite from something big."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36e71d16-0964-489d-bea2-9cec7991fc99.jpg?1783925653"
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

private val HookHauntDrifter = card("Hook-Haunt Drifter") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "If Hook-Haunt Drifter would be put into a graveyard from anywhere, exile it instead."

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
        collectorNumber = "42"
        artist = "Uriah Voth"
        flavorText = "Even in death, he casts his lines, forever fishing for reasons long forgotten."
        imageUri = "https://cards.scryfall.io/normal/back/3/6/36e71d16-0964-489d-bea2-9cec7991fc99.jpg?1783925653"
    }
}

val BaithookAngler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BaithookAnglerFront,
    backFace = HookHauntDrifter,
)
