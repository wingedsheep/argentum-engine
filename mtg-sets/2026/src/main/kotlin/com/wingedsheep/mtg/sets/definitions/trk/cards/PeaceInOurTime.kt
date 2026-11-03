package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Peace in Our Time
 * {3}{W}
 * Enchantment
 * At the beginning of combat on each player's turn, that player may draw a card. If they do, they
 * can't attack this turn and you put a peace counter on this enchantment.
 * At the beginning of your upkeep, if this enchantment has twelve or more peace counters on it, you
 * win the game.
 *
 *  - "That player" is the active player the step trigger binds as its triggering player, so the
 *    may-draw is theirs to decide and the card goes to their hand.
 *  - "They can't attack this turn" is a live-filter rule-modifying effect over the creatures the
 *    active player controls until end of turn, so a creature they gain or cast afterwards can't
 *    attack either, nor can any creature in an additional combat phase that turn.
 *  - The win check is an intervening "if", read as the upkeep begins and again on resolution.
 */
val PeaceInOurTime = card("Peace in Our Time") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on each player's turn, that player may draw a card. If they do, " +
        "they can't attack this turn and you put a peace counter on this enchantment.\n" +
        "At the beginning of your upkeep, if this enchantment has twelve or more peace counters on it, " +
        "you win the game."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        val thatPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        effect = Effects.May(
            effect = Effects.DrawCards(1, thatPlayer) then
                Effects.CantAttackGroup(
                    GroupFilter(GameObjectFilter.Creature.controlledByActivePlayer()),
                    Duration.EndOfTurn
                ) then
                Effects.AddCounters(CounterType.PEACE, 1, EffectTarget.Self),
            decisionMaker = thatPlayer,
            prompt = "Draw a card? If you do, you can't attack this turn.",
            descriptionOverride = "That player may draw a card. If they do, they can't attack this turn " +
                "and you put a peace counter on this enchantment."
        )
        description = "At the beginning of combat on each player's turn, that player may draw a card. If they " +
            "do, they can't attack this turn and you put a peace counter on this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceCounterCountAtLeast(CounterType.PEACE, 12)
        effect = Effects.WinGame()
        description = "At the beginning of your upkeep, if this enchantment has twelve or more peace counters " +
            "on it, you win the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Elizabeth Peiró"
        flavorText = "Sometimes, hope is a choice."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dd522dd-a894-496e-9d31-68583dba7ecc.jpg?1792508151"
    }
}
