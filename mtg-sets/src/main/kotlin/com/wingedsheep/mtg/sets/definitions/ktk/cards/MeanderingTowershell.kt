package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Meandering Towershell
 * {3}{G}{G}
 * Creature — Turtle
 * 5/9
 * Islandwalk
 * Whenever Meandering Towershell attacks, exile it. Return it to the battlefield
 * under your control tapped and attacking at the beginning of the declare attackers
 * step on your next turn.
 */
val MeanderingTowershell = card("Meandering Towershell") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Turtle"
    power = 5
    toughness = 9
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
            "Whenever Meandering Towershell attacks, exile it. Return it to the battlefield under your control " +
            "tapped and attacking at the beginning of the declare attackers step on your next turn."

    keywords(Keyword.ISLANDWALK)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                Effects.Move(EffectTarget.Self, Zone.EXILE),
                CreateDelayedTriggerEffect(
                    step = Step.BEGIN_COMBAT,
                    effect = Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.TappedAndAttacking,
                        controllerOverride = EffectTarget.Controller
                    ),
                    fireOnlyOnControllersTurn = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a945b43d-39bc-4ce4-be03-0109e1a681b1.jpg?1562791660"
        ruling("2014-09-20", "As Meandering Towershell returns to the battlefield because of the delayed triggered ability, you choose which opponent or opposing planeswalker it's attacking.")
        ruling("2014-09-20", "If Meandering Towershell enters the battlefield attacking, it wasn't declared as an attacking creature that turn.")
        ruling("2014-09-20", "On the turn Meandering Towershell attacks and is exiled, raid abilities will see it as a creature that attacked.")
    }
}
