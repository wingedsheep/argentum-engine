package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daring Sleuth // Bearer of Overwhelming Truths (SOI #54)
 *
 * Front — {1}{U} Creature — Human Rogue 2/1
 *   When you sacrifice a Clue, transform this creature.
 * Back — Creature — Human Wizard 3/2 (blue color indicator)
 *   Prowess
 *   Whenever this creature deals combat damage to a player, investigate.
 *
 * "Sacrifice a Clue" is the per-permanent template (`Triggers.you.sacrifices`, CR 603.2c) keyed on the
 * Clue artifact subtype, so any Clue sacrificed for any reason counts. When several of these triggers
 * are on the stack at once, only the first to resolve transforms the Sleuth: the engine freezes the
 * source's face-change tally as each trigger goes on the stack and `Effects.Transform` does nothing once
 * the permanent has transformed since (CR 701.27g).
 */
private val DaringSleuthFront = card("Daring Sleuth") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "When you sacrifice a Clue, transform this creature."

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Clue"))
        effect = Effects.Transform(EffectTarget.Self)
        description = "When you sacrifice a Clue, transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Seb McKinnon"
        flavorText = "\"No matter the cost, I must discover the source of these strange happenings.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/440f9f10-6999-4cff-ab98-8aed77813b07.jpg?1783937809"

        ruling(
            "2016-04-08",
            "If Daring Sleuth's ability triggers multiple times before any of those abilities resolves, " +
                "only the first one to resolve will cause it to transform."
        )
        ruling(
            "2016-04-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact token."
        )
    }
}

private val BearerOfOverwhelmingTruths = card("Bearer of Overwhelming Truths") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Whenever this creature deals combat damage to a player, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    prowess()

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Investigate()
        description = "Whenever this creature deals combat damage to a player, investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/back/4/4/440f9f10-6999-4cff-ab98-8aed77813b07.jpg?1783937809"
    }
}

val DaringSleuth: CardDefinition = CardDefinition.doubleFacedCreature(DaringSleuthFront, BearerOfOverwhelmingTruths)
