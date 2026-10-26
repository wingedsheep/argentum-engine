package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Norn's Decree
 * {2}{W}
 * Enchantment
 * Whenever one or more creatures an opponent controls deal combat damage to you, that opponent
 * gets a poison counter.
 * Whenever a player attacks, if one or more players being attacked are poisoned, the attacking
 * player draws a card.
 *
 * The first ability fires once per opponent whose creatures hit you in a combat damage step
 * (`perSourceController`), binding that opponent as the triggering player. The second is any
 * attack declaration (`Triggers.anyPlayer.attacks()`), whose triggering player is the attacking
 * player; the intervening-if asks whether any player some creature is attacking directly
 * (`Player.EachAttackedPlayer`) has a poison counter — a player whose planeswalker alone is attacked
 * isn't "being attacked" — checked when it triggers and again on resolution.
 */
val NornsDecree = card("Norn's Decree") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures an opponent controls deal combat damage to you, that opponent " +
        "gets a poison counter.\n" +
        "Whenever a player attacks, if one or more players being attacked are poisoned, the attacking " +
        "player draws a card."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.opponentControls())
            .dealCombatDamageToYou(perSourceController = true)
        effect = Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever one or more creatures an opponent controls deal combat damage to you, " +
            "that opponent gets a poison counter."
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.greatestAmongPlayers(
                DynamicAmounts.playerCounterCount(CounterType.POISON),
                players = Player.EachAttackedPlayer,
            ),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever a player attacks, if one or more players being attacked are poisoned, " +
            "the attacking player draws a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76f28aa2-1c18-4d58-a3f9-6cd5b49280d8.jpg?1783918161"
        ruling("2023-02-04", "A player is poisoned if that player has one or more poison counters.")
        ruling(
            "2023-02-04",
            "A player with ten or more poison counters loses the game. This is a state-based action and " +
                "doesn't use the stack. In other words, it happens immediately and players can't respond to " +
                "it, just like a player losing the game due to having 0 or less life."
        )
    }
}
