package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elusive Tormentor // Insidious Mist (Shadows over Innistrad #108)
 * {2}{B}{B}
 * Creature — Vampire Wizard 4/4 // Creature — Elemental 0/1
 *
 * Front — "{1}, Discard a card: Transform this creature."
 * Back  — "Hexproof, indestructible / This creature can't block and can't be blocked. /
 *          Whenever this creature attacks and isn't blocked, you may pay {2}{B}. If you do,
 *          transform it."
 *
 * The back face has no mana cost, so its blue comes from a color indicator (CR 204); the card's
 * color identity is therefore blue-black. Transforming mid-combat keeps the creature attacking and
 * keeps its unblocked status, so flipping back to the 4/4 after the trigger resolves lets it deal
 * 4 combat damage. Stacked transform abilities only flip it once (CR 701.28f, enforced by the
 * transform executor).
 */

private val ElusiveTormentorFront = card("Elusive Tormentor") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "BU"
    typeLine = "Creature — Vampire Wizard"
    power = 4
    toughness = 4
    oracleText = "{1}, Discard a card: Transform this creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.DiscardCard)
        effect = Effects.Transform(EffectTarget.Self)
        description = "{1}, Discard a card: Transform this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Izzy"
        flavorText = "Older vampires are usually more cautious than the newly-sired, calling upon ancient " +
            "magic for which neonates lack patience."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a32ce90d-9138-4aca-aa3c-a558dd16fad8.jpg?1783937784"
        ruling(
            "2016-04-08",
            "You can activate Elusive Tormentor's ability multiple times to discard multiple cards. " +
                "Only the first instance of the ability to resolve will cause it to transform."
        )
        ruling(
            "2016-04-08",
            "You can, all within one turn, attack with Elusive Tormentor, transform it into Insidious " +
                "Mist before blockers are chosen, transform it back as its triggered ability resolves, " +
                "and have it deal 4 combat damage."
        )
        ruling(
            "2016-04-08",
            "Once blockers have been chosen, transforming Elusive Tormentor into Insidious Mist won't " +
                "cause it to become unblocked."
        )
    }
}

private val InsidiousMist = card("Insidious Mist") {
    manaCost = ""
    colorIdentity = "BU"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 1
    oracleText = "Hexproof, indestructible\n" +
        "This creature can't block and can't be blocked.\n" +
        "Whenever this creature attacks and isn't blocked, you may pay {2}{B}. If you do, transform it."

    keywords(Keyword.HEXPROOF, Keyword.INDESTRUCTIBLE)

    staticAbility {
        ability = CantBlock()
    }

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.self.attacksAndIsntBlocked()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{B}"),
            then = Effects.Transform(EffectTarget.Self)
        )
        description = "Whenever this creature attacks and isn't blocked, you may pay {2}{B}. " +
            "If you do, transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/back/a/3/a32ce90d-9138-4aca-aa3c-a558dd16fad8.jpg?1783937784"
    }
}

val ElusiveTormentor: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ElusiveTormentorFront,
    backFace = InsidiousMist,
)
