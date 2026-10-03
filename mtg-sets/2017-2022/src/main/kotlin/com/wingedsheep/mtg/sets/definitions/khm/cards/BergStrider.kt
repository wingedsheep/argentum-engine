package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Berg Strider
 * {4}{U}
 * Snow Creature — Giant Wizard
 * 4/4
 * When this creature enters, tap target artifact or creature an opponent controls. If {S} was
 * spent to cast this spell, that permanent doesn't untap during its controller's next untap step.
 *
 * "{S} was spent" is any mana from a snow source spent on the cost (CR 107.4h) — Berg Strider has
 * no {S} pip of its own — read off the spell's recorded payment by `SnowManaSpent`. A Berg Strider
 * put onto the battlefield without being cast spent no mana, so it only taps.
 */
val BergStrider = card("Berg Strider") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Giant Wizard"
    oracleText = "When this creature enters, tap target artifact or creature an opponent controls. " +
        "If {S} was spent to cast this spell, that permanent doesn't untap during its controller's next " +
        "untap step. ({S} is mana from a snow source.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))
        effect = Effects.Tap(permanent) then
            Effects.If(
                condition = Conditions.CompareAmounts(DynamicAmounts.snowManaSpent(), ComparisonOperator.GTE, 1),
                then = Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    permanent,
                    Duration.UntilAfterAffectedControllersNextUntap,
                ),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3567bdc-450e-4481-9349-a80fe52fe431.jpg?1783928268"
    }
}
