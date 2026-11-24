package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Martial Law
 * {2}{W}{W}
 * Enchantment
 * At the beginning of your upkeep, detain target creature an opponent controls. (Until your next
 * turn, that creature can't attack or block and its activated abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's only printing.
 *
 * Detain is spelled out as in Azorius Arrester: can't attack or block until your next turn, plus a
 * granted activation ban for the same duration.
 */
val MartialLaw = card("Martial Law") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, detain target creature an opponent controls. (Until " +
        "your next turn, that creature can't attack or block and its activated abilities can't be activated.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.UntilYourNextTurn,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Tyler Jacobson"
        flavorText = "\"The good of society matters much more than the inconvenience of a few.\"\n—Agmand Sarv, Azorius hussar"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21078b6f-a39d-4ec8-879e-ad10d97c3ff6.jpg?1783940374"
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
