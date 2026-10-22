package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chaplain of Alms // Chapel Shieldgeist (Innistrad: Midnight Hunt #13 — the card's earliest
 * printing; also reprinted in Innistrad: Double Feature)
 * {W} · Creature — Human Cleric 1/1 // Creature — Spirit Cleric 2/1
 *
 * Front — Chaplain of Alms ({W}, Creature — Human Cleric, 1/1)
 *   First strike
 *   Ward {1}
 *   Disturb {3}{W}
 *
 * Back — Chapel Shieldgeist (Creature — Spirit Cleric, 2/1, white color indicator)
 *   Flying, first strike
 *   Each creature you control has ward {1}.
 *   If Chapel Shieldgeist would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation:
 *  - Disturb (CR 702.146) is the [disturb] keyword on the front face; the card is cast from the
 *    graveyard back face up for {3}{W}.
 *  - "Each creature you control has ward {1}" is [GrantWard] over every creature you control —
 *    no `.other()`, so the Shieldgeist itself has ward {1} too.
 *  - The graveyard-to-exile clause is the self-only [RedirectZoneChange], which works from every
 *    zone including the stack (a countered disturb spell is exiled).
 */
private val ChaplainOfAlmsFront = card("Chaplain of Alms") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "First strike\n" +
        "Ward {1} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "Disturb {3}{W} (You may cast this card from your graveyard transformed for its disturb cost.)"

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    disturb("{3}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20e94e17-2e4c-41cd-8cc5-39ab41037287.jpg?1783925671"
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
                "to exile if it would be put into a graveyard from anywhere. This includes going to " +
                "the graveyard from the stack, so if the spell is countered after you cast it using " +
                "the disturb ability, it will be put into exile."
        )
    }
}

private val ChapelShieldgeist = card("Chapel Shieldgeist") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Spirit Cleric"
    power = 2
    toughness = 1
    oracleText = "Flying, first strike\n" +
        "Each creature you control has ward {1}. (Whenever it becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "If Chapel Shieldgeist would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Mana("{1}"),
            filter = GroupFilter.AllCreaturesYouControl,
        )
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/back/2/0/20e94e17-2e4c-41cd-8cc5-39ab41037287.jpg?1783925671"
    }
}

val ChaplainOfAlms: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ChaplainOfAlmsFront,
    backFace = ChapelShieldgeist,
)
