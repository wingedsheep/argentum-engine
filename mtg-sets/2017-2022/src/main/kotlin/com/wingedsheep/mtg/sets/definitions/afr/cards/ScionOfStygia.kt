package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scion of Stygia
 * {2}{U}
 * Creature — Tiefling Shaman
 * 2/1
 * Flash
 * Cone of Cold — When this creature enters, choose target creature an opponent controls,
 * then roll a d20.
 * 1—9 | Tap that creature.
 * 10—20 | Tap that creature. It doesn't untap during its controller's next untap step.
 */
val ScionOfStygia = card("Scion of Stygia") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Tiefling Shaman"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "Cone of Cold — When this creature enters, choose target creature an opponent controls, then roll a d20.\n" +
        "1—9 | Tap that creature.\n" +
        "10—20 | Tap that creature. It doesn't untap during its controller's next untap step."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9)) {
                run(Effects.Tap(creature))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10)) {
                run(Effects.Tap(creature))
                run(
                    Effects.GrantKeyword(
                        AbilityFlag.DOESNT_UNTAP,
                        creature,
                        Duration.UntilAfterAffectedControllersNextUntap(),
                    )
                )
            }
        }
        description = "Cone of Cold — When this creature enters, choose target creature an opponent controls, " +
            "then roll a d20. 1—9: tap that creature. 10—20: tap that creature; it doesn't untap during " +
            "its controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f0abdb4-4898-4818-b7e2-24a90f15e49d.jpg?1783926509"
        ruling("2021-07-23", "An effect that says \"choose a target, then roll a d20\" or similar still uses the normal process of putting an ability on the stack and resolving it. Choosing targets is part of putting the ability on the stack and rolling the d20 happens later, as the ability resolves.")
    }
}
