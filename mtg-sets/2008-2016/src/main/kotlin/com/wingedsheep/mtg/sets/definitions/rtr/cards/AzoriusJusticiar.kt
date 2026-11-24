package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Azorius Justiciar
 * {2}{W}{W}
 * Creature — Human Wizard
 * 2/2
 * When this creature enters, detain up to two target creatures your opponents control. (Until your
 * next turn, those creatures can't attack or block and their activated abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Detain is spelled out per target as in Archon of the Triumvirate: can't attack or block until
 * your next turn, plus a granted activation ban for the same duration.
 */
val AzoriusJusticiar = card("Azorius Justiciar") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, detain up to two target creatures your opponents control. " +
        "(Until your next turn, those creatures can't attack or block and their activated abilities " +
        "can't be activated.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.CreatureOpponentControls, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.CantAttackOrBlock(EffectTarget.ContextTarget(0), Duration.UntilYourNextTurn) then
                Effects.GrantStaticAbility(
                    ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                    target = EffectTarget.ContextTarget(0),
                    duration = Duration.UntilYourNextTurn,
                )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Chris Rahn"
        flavorText = "\"Your potential to commit a crime warrants further investigation.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f56272e-c05e-446b-8871-e3783dd29a8b.jpg?1783940377"
        ruling("2012-10-01", "The two creatures can be controlled by the same opponent or by different opponents.")
        ruling(
            "2013-04-15",
            "Activated abilities include a colon and are written in the form \"[cost]: [effect].\" No one can activate any activated abilities, including mana abilities, of a detained permanent."
        )
        ruling("2013-04-15", "The static abilities of a detained permanent still apply. The triggered abilities of a detained permanent can still trigger.")
        ruling("2013-04-15", "If a creature is already attacking or blocking when it's detained, it won't be removed from combat. It will continue to attack or block.")
        ruling("2013-04-15", "If a permanent's activated ability is on the stack when that permanent is detained, the ability will be unaffected.")
        ruling("2013-04-15", "If a noncreature permanent is detained and later turns into a creature, it won't be able to attack or block.")
        ruling("2013-04-15", "When a player leaves a multiplayer game, any continuous effects with durations that last until that player's next turn or until a specific point in that turn will last until that turn would have begun. They neither expire immediately nor last indefinitely.")
    }
}
