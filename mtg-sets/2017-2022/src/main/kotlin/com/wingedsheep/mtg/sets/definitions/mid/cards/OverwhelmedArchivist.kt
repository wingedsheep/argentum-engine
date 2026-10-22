package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Overwhelmed Archivist // Archive Haunt (Innistrad: Midnight Hunt #68)
 * {2}{U} · Creature — Human Wizard 3/2 // Creature — Spirit Wizard 2/1
 *
 * Front — Overwhelmed Archivist ({2}{U}, Creature — Human Wizard, 3/2)
 *   When this creature enters, draw a card, then discard a card.
 *   Disturb {3}{U}
 *
 * Back — Archive Haunt (Creature — Spirit Wizard, 2/1, blue color indicator)
 *   Flying
 *   Whenever this creature attacks, draw a card, then discard a card.
 *   If Archive Haunt would be put into a graveyard from anywhere, exile it instead.
 */
private val OverwhelmedArchivistFront = card("Overwhelmed Archivist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, draw a card, then discard a card.\n" +
        "Disturb {3}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot()
    }

    disturb("{3}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Cristi Balanescu"
        flavorText = "\"Bah! As if I had time to preserve everything.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/832288fd-8031-4c2b-ad3e-b1ec9f94d379.jpg?1783925639"
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
                "to exile if it would be put into a graveyard from anywhere. This includes going " +
                "to the graveyard from the stack, so if the spell is countered after you cast it " +
                "using the disturb ability, it will be put into exile."
        )
    }
}

private val ArchiveHaunt = card("Archive Haunt") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Spirit Wizard"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature attacks, draw a card, then discard a card.\n" +
        "If Archive Haunt would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Hand.loot()
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
        collectorNumber = "68"
        artist = "Cristi Balanescu"
        flavorText = "Cleaning up his life's work was his afterlife's work."
        imageUri = "https://cards.scryfall.io/normal/back/8/3/832288fd-8031-4c2b-ad3e-b1ec9f94d379.jpg?1783925639"
    }
}

val OverwhelmedArchivist: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = OverwhelmedArchivistFront,
    backFace = ArchiveHaunt,
)
