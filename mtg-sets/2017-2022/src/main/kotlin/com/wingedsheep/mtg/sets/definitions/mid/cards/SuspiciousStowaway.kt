package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Suspicious Stowaway // Seafaring Werewolf (Innistrad: Midnight Hunt)
 * {1}{U}
 * Creature — Human Rogue Werewolf // Creature — Werewolf
 *
 * Front — Suspicious Stowaway (1/1): Can't be blocked. Whenever it deals combat damage to a
 * player, draw a card, then discard a card. Daybound.
 * Back  — Seafaring Werewolf (2/1): Can't be blocked. Whenever it deals combat damage to a
 * player, draw a card. Nightbound.
 *
 * The back face carries a green colour indicator as printed (Scryfall: color_indicator G), so the
 * card's colour identity is {U}{G}.
 */

private val SuspiciousStowawayFront = card("Suspicious Stowaway") {
    manaCost = "{1}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Human Rogue Werewolf"
    power = 1
    toughness = 1
    oracleText = "This creature can't be blocked.\n" +
        "Whenever this creature deals combat damage to a player, draw a card, then discard a card.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
        description = "Whenever this creature deals combat damage to a player, draw a card, then discard a card."
    }

    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab17c8fa-4c06-4542-848a-e3f2f9f47c27.jpg?1783925634"
    }
}

private val SeafaringWerewolf = card("Seafaring Werewolf") {
    manaCost = ""
    colorIdentity = "UG"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 2
    toughness = 1
    oracleText = "This creature can't be blocked.\n" +
        "Whenever this creature deals combat damage to a player, draw a card.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
        description = "Whenever this creature deals combat damage to a player, draw a card."
    }

    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/back/a/b/ab17c8fa-4c06-4542-848a-e3f2f9f47c27.jpg?1783925634"
    }
}

val SuspiciousStowaway: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SuspiciousStowawayFront,
    backFace = SeafaringWerewolf,
)
