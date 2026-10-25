package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodchief Ascension — Zendikar #82
 * {B} · Enchantment · Rare
 *
 * At the beginning of each end step, if an opponent lost 2 or more life this turn, you may put a
 * quest counter on this enchantment. (Damage causes loss of life.)
 * Whenever a card is put into an opponent's graveyard from anywhere, if this enchantment has three
 * or more quest counters on it, you may have that player lose 2 life. If you do, you gain 2 life.
 *
 * Modeling notes:
 *  - "An opponent lost 2 or more life" is a per-opponent threshold (ruling: two opponents losing 1
 *    each doesn't count), so it is `countPlayersWith(EachOpponent, lifeLostThisTurn() >= 2) >= 1`
 *    — [DynamicAmounts.countPlayersWith] rebinds `Player.You` to each opponent in turn — rather than
 *    a summed `TurnTracking` over every opponent. Life lost counts regardless of life gained.
 *  - The graveyard trigger mirrors Vulturous Zombie: a per-card `ZoneChangeEvent` to the graveyard
 *    with no `from` zone, nontoken (a token isn't a card), keyed on the opponent who **owns** the
 *    card; "that player" is [Player.OwnerOfTriggeringEntity].
 */
val BloodchiefAscension = card("Bloodchief Ascension") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each end step, if an opponent lost 2 or more life this turn, " +
        "you may put a quest counter on this enchantment. (Damage causes loss of life.)\n" +
        "Whenever a card is put into an opponent's graveyard from anywhere, if this enchantment has " +
        "three or more quest counters on it, you may have that player lose 2 life. If you do, you " +
        "gain 2 life."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countPlayersWith(
                scope = Player.EachOpponent,
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.lifeLostThisTurn(),
                    ComparisonOperator.GTE,
                    2
                )
            ),
            ComparisonOperator.GTE,
            1
        )
        optional = true
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
        description = "At the beginning of each end step, if an opponent lost 2 or more life this " +
            "turn, you may put a quest counter on this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Any
                .withCardPredicate(CardPredicate.IsNontoken)
                .ownedByOpponent()
        ).changesZone(to = Zone.GRAVEYARD)
        interveningIf = Conditions.SourceCounterCountAtLeast(CounterType.QUEST, 3)
        optional = true
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.OwnerOfTriggeringEntity)) then
            Effects.GainLife(2)
        description = "Whenever a card is put into an opponent's graveyard from anywhere, if this " +
            "enchantment has three or more quest counters on it, you may have that player lose 2 " +
            "life. If you do, you gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Adi Granov"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa213dbb-c52a-4084-92aa-d0b5d97a97d9.jpg?1783942156"
        ruling(
            "2009-10-01",
            "Bloodchief Ascension's first ability has an \"intervening 'if' clause.\" It won't trigger at all unless an opponent has already lost 2 or more life by the time the end step begins."
        )
        ruling(
            "2009-10-01",
            "Bloodchief Ascension's second ability doesn't behave like a leaves-the-battlefield triggered ability, since the card put into an opponent's graveyard may come from anywhere. If a Bloodchief Ascension with three quest counters on it and a permanent an opponent owns are destroyed at the same time, for example, the game will not \"look back in time\" at the game state, and Bloodchief Ascension's second ability won't trigger."
        )
        ruling(
            "2009-10-01",
            "Bloodchief Ascension's first ability checks how much life was lost by each opponent over the course of the entire turn, even if Bloodchief Ascension wasn't on the battlefield the whole time."
        )
        ruling(
            "2009-10-01",
            "For the first ability to trigger, a single opponent must have lost 2 life. Two opponents each losing 1 life won't cause it to trigger. It will trigger a maximum of once per turn, no matter how many opponents have lost 2 or more life."
        )
        ruling(
            "2009-10-01",
            "Bloodchief Ascension's first ability checks only whether life was lost. It doesn't care whether life was also gained."
        )
    }
}
