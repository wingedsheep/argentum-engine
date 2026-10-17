package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Celestial Regulator
 * {1}{W}{U}
 * Creature — Angel Advisor
 * 2/3
 * Flying
 * When this creature enters, choose target creature you don't control and tap it. If you control
 * a creature with a counter on it, the chosen creature doesn't untap during its controller's next
 * untap step.
 *
 * Same shape as Berg Strider: tap, then a resolution-time [Effects.If] that grants
 * DOESNT_UNTAP through the affected creature's controller's next untap step.
 */
val CelestialRegulator = card("Celestial Regulator") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Angel Advisor"
    oracleText = "Flying\n" +
        "When this creature enters, choose target creature you don't control and tap it. " +
        "If you control a creature with a counter on it, the chosen creature doesn't untap " +
        "during its controller's next untap step."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Tap(creature) then
            Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.Creature.withAnyCounter()),
                then = Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    creature,
                    Duration.UntilAfterAffectedControllersNextUntap(),
                ),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25803f0b-4475-447e-abdf-dcd6a98dd654.jpg?1783923092"
    }
}
