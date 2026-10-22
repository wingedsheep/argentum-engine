package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Covert Cutpurse // Covetous Geist (Innistrad: Midnight Hunt #92)
 * {2}{B} · Creature — Human Rogue 2/1 // Creature — Spirit Rogue 2/2
 *
 * Front — Covert Cutpurse ({2}{B}, Creature — Human Rogue, 2/1)
 *   When this creature enters, destroy target creature you don't control that was dealt damage this turn.
 *   Disturb {4}{B}
 *
 * Back — Covetous Geist (Creature — Spirit Rogue, 2/2, black color indicator)
 *   Flying, deathtouch
 *   If Covetous Geist would be put into a graveyard from anywhere, exile it instead.
 *
 * "you don't control" is spelled `Not(ControlledByYou)` rather than `ControlledByOpponent` — the
 * two diverge in Two-Headed Giant, where a teammate's creature is one you don't control.
 */
private val CovertCutpurseFront = card("Covert Cutpurse") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, destroy target creature you don't control that was dealt damage this turn.\n" +
        "Disturb {4}{B} (You may cast this card from your graveyard transformed for its disturb cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.wasDealtDamageThisTurn().copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Destroy(t)
    }

    disturb("{4}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5db99746-8aee-42b8-acb0-ed69933d0ff8.jpg?1783925627"
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

private val CovetousGeist = card("Covetous Geist") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Spirit Rogue"
    power = 2
    toughness = 2
    oracleText = "Flying, deathtouch\n" +
        "If Covetous Geist would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "John Stanko"
        flavorText = "\"It has no need of wealth in the afterlife. It steals and kills purely for its own " +
            "spiteful amusement.\"\n—Kaya"
        imageUri = "https://cards.scryfall.io/normal/back/5/d/5db99746-8aee-42b8-acb0-ed69933d0ff8.jpg?1783925627"
    }
}

val CovertCutpurse: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CovertCutpurseFront,
    backFace = CovetousGeist,
)
