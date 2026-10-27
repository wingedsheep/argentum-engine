package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Breena, the Demagogue — Commander 2021 #1
 * {1}{W}{B} · Legendary Creature — Bird Warlock · 1/3
 *
 * Flying
 * Whenever a player attacks one of your opponents, if that opponent has more life than another of
 * your opponents, that attacking player draws a card and you put two +1/+1 counters on a creature
 * you control.
 *
 * The trigger fires once per opponent attacked (`Triggers.anyPlayer.attacksAPlayer(attacked =
 * EachOpponent)`), binding the attacked opponent as `TriggeringPlayer`; "that attacking player" is
 * the active player, the only one who declares attackers. "More life than another of your
 * opponents" is "more than the fewest life among your opponents" — the attacked opponent is one of
 * them, so it only fails when no other opponent has less (and the attacking player counts when it
 * is an opponent, per the ruling). The creature is chosen on resolution, not targeted.
 */
val BreenaTheDemagogue = card("Breena, the Demagogue") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Bird Warlock"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever a player attacks one of your opponents, if that opponent has more life than another " +
        "of your opponents, that attacking player draws a card and you put two +1/+1 counters on a " +
        "creature you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.attacksAPlayer(attacked = Player.EachOpponent)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.lifeTotal(Player.TriggeringPlayer),
            ComparisonOperator.GT,
            DynamicAmounts.leastAmongPlayers(DynamicAmounts.yourLifeTotal(), Player.EachOpponent)
        )
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ActivePlayer)))
            val chosen = selectTarget(
                TargetObject(filter = TargetFilter.CreatureYouControl),
                nonTargeting = true,
                prompt = "Choose a creature you control to get two +1/+1 counters"
            )
            run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, chosen.asTarget))
        }
        description = "Whenever a player attacks one of your opponents, if that opponent has more life than " +
            "another of your opponents, that attacking player draws a card and you put two +1/+1 counters " +
            "on a creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cb82b10-d785-4e20-84ac-0660d49be8ad.jpg?1783927617"
        ruling("2021-04-16", "Breena's triggered ability will trigger even if you control no creatures.")
        ruling(
            "2021-04-16",
            "If the attacking player is one of your opponents and has less life than the player being attacked, " +
                "then the condition is satisfied."
        )
        ruling(
            "2021-04-16",
            "You choose which creature to put +1/+1 counters on as the ability is resolving. Once you make the " +
                "choice, no player may take actions until the counters are placed."
        )
    }
}
