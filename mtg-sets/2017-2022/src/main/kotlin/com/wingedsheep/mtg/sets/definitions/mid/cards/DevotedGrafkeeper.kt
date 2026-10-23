package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Devoted Grafkeeper // Departed Soulkeeper (Innistrad: Midnight Hunt #218)
 * {W}{U} · Creature — Human Peasant 2/1 // Creature — Spirit 3/1
 *
 * Front — Devoted Grafkeeper ({W}{U}, Creature — Human Peasant, 2/1)
 *   When this creature enters, mill two cards.
 *   Whenever you cast a spell from your graveyard, tap target creature you don't control.
 *   Disturb {1}{W}{U}
 *
 * Back — Departed Soulkeeper (Creature — Spirit, 3/1, white and blue color indicator)
 *   Flying
 *   This creature can block only creatures with flying.
 *   If Departed Soulkeeper would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation:
 *  - The cast trigger is `Triggers.you.casts` gated on `SpellCastPredicate.CastFromZone(GRAVEYARD)`
 *    (as on Ominous Roost), so flashback, disturb and any other graveyard cast all feed it.
 *  - "you don't control" is `Not(ControlledByYou)` rather than `ControlledByOpponent`, as on
 *    Covert Cutpurse — the two diverge in Two-Headed Giant.
 */
private val DevotedGrafkeeperFront = card("Devoted Grafkeeper") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, mill two cards.\n" +
        "Whenever you cast a spell from your graveyard, tap target creature you don't control.\n" +
        "Disturb {1}{W}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(2)
        description = "When this creature enters, mill two cards."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.GRAVEYARD)))
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Tap(t)
        description = "Whenever you cast a spell from your graveyard, tap target creature you don't control."
    }

    disturb("{1}{W}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/965e6bd5-dc32-406c-bc99-ceb15be4d3f2.jpg?1783925570"
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

private val DepartedSoulkeeper = card("Departed Soulkeeper") {
    manaCost = ""
    colorIdentity = "WU"
    colorIndicator = "WU"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 1
    oracleText = "Flying\n" +
        "This creature can block only creatures with flying.\n" +
        "If Departed Soulkeeper would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
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
        collectorNumber = "218"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/back/9/6/965e6bd5-dc32-406c-bc99-ceb15be4d3f2.jpg?1783925570"
    }
}

val DevotedGrafkeeper: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DevotedGrafkeeperFront,
    backFace = DepartedSoulkeeper,
)
