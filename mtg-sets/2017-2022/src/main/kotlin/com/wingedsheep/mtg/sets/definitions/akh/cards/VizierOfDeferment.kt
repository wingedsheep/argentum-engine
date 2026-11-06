package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Vizier of Deferment
 * {2}{W}
 * Creature — Human Cleric
 * 2/2
 * Flash
 * When this creature enters, you may exile target creature if it attacked or blocked this turn.
 * Return that card to the battlefield under its owner's control at the beginning of the next end step.
 *
 * The ability may target any creature; whether it attacked or blocked is checked on resolution
 * ([Conditions.EntityMatches] over the `AttackedThisTurn` / `BlockedThisTurn` turn markers), and only
 * then is the controller asked whether to exile it. [Patterns.Exile.exileUntilEndStep] exiles it and
 * schedules the delayed return under its owner's control.
 */
val VizierOfDeferment = card("Vizier of Deferment") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, you may exile target creature if it attacked or blocked this turn. " +
        "Return that card to the battlefield under its owner's control at the beginning of the next end step."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                creature,
                GameObjectFilter.Creature.withStatePredicate(
                    StatePredicate.Or(listOf(StatePredicate.AttackedThisTurn, StatePredicate.BlockedThisTurn))
                ),
            ),
            then = Effects.May(Patterns.Exile.exileUntilEndStep(creature)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2684f2e5-e3f9-4277-94a1-aba6913ac53b.jpg?1783936529"
    }
}
