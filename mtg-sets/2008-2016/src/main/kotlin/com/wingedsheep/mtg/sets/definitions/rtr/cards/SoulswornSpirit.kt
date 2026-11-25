package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soulsworn Spirit
 * {3}{U}
 * Creature — Spirit
 * 2/1
 * This creature can't be blocked.
 * When this creature enters, detain target creature an opponent controls. (Until your next turn,
 * that creature can't attack or block and its activated abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's only printing.
 *
 * Detain is spelled out as in Azorius Arrester: can't attack or block until your next turn, plus a
 * granted activation ban for the same duration.
 */
val SoulswornSpirit = card("Soulsworn Spirit") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "This creature can't be blocked.\nWhen this creature enters, detain target creature an " +
        "opponent controls. (Until your next turn, that creature can't attack or block and its activated " +
        "abilities can't be activated.)"

    flags(AbilityFlag.CANT_BE_BLOCKED)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.UntilYourNextTurn,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32602ed9-c0a7-4498-a333-235eaae628df.jpg?1783940366"
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
