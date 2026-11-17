package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temur War Shaman
 * {4}{G}{G}
 * Creature — Human Shaman
 * 4/5
 * When this creature enters, manifest the top card of your library.
 * Whenever a permanent you control is turned face up, if it's a creature, you may have it fight
 * target creature you don't control.
 *
 * The turn-face-up trigger reads any permanent you control — the Shaman's own manifest, a morph, or
 * the Shaman itself if it was face down — so there is no "another" filter. "If it's a creature" is an
 * intervening-if on the flipped permanent's face-up characteristics, checked when the trigger fires
 * and again on resolution (CR 603.4); "you may have it fight" is an optional trigger whose fight
 * runs from the triggering permanent against the chosen target. A manifested instant or sorcery
 * never turns face up, so it never triggers this (ruling).
 */
val TemurWarShaman = card("Temur War Shaman") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, manifest the top card of your library. (Put that card " +
        "onto the battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost " +
        "if it's a creature card.)\n" +
        "Whenever a permanent you control is turned face up, if it's a creature, you may have it fight " +
        "target creature you don't control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.manifest()
        description = "When Temur War Shaman enters, manifest the top card of your library."
    }

    triggeredAbility {
        trigger = Triggers.you.permanentTurnedFaceUp()
        interveningIf = Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Creature)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        optional = true
        effect = Effects.Fight(EffectTarget.TriggeringEntity, theirs)
        description = "Whenever a permanent you control is turned face up, if it's a creature, you may " +
            "have it fight target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a873164f-d8eb-460e-a8cc-8b2a663dcf3a.jpg?1783938678"
        ruling(
            "2014-11-24",
            "Any time you have priority, you may turn a manifested creature face up by revealing that " +
                "it's a creature card (ignoring any copy effects or type-changing effects that might be " +
                "applying to it) and paying its mana cost. This is a special action. It doesn't use the " +
                "stack and can't be responded to."
        )
        ruling(
            "2014-11-24",
            "If a manifested creature would have morph if it were face up, you may also turn it face up " +
                "by paying its morph cost."
        )
        ruling(
            "2014-11-24",
            "Because the permanent is on the battlefield both before and after it's turned face up, " +
                "turning a permanent face up doesn't cause any enters-the-battlefield abilities to trigger."
        )
        ruling(
            "2014-11-24",
            "If something tries to turn a face-down instant or sorcery card on the battlefield face up, " +
                "reveal that card to show all players it's an instant or sorcery card. The permanent " +
                "remains on the battlefield face down. Abilities that trigger when a permanent turns face " +
                "up won't trigger, because even though you revealed the card, it never turned face up."
        )
    }
}
