package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Watertrap Weaver
 * {2}{U}
 * Creature — Merfolk Wizard
 * 2/2
 *
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's next untap step.
 */
val WatertrapWeaver = card("Watertrap Weaver") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, tap target creature an opponent controls. That creature " +
        "doesn't untap during its controller's next untap step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Josu Hernaiz"
        flavorText = "The river is a powerful friend."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d973568-d7b0-443f-a09d-44f6b02da5d4.jpg?1783935768"
    }
}
