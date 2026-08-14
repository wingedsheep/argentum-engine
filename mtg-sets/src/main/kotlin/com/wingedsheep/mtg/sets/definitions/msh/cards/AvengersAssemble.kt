package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Avengers Assemble! — Marvel Super Heroes #6 (mythic)
 * {4}{W} · Enchantment
 *
 * Flash
 * Heroes you control get +2/+2.
 * At the beginning of each end step, if you attacked with a Hero this turn or a Hero entered
 * the battlefield under your control this turn, draw a card.
 *
 * Implementation notes:
 * - The anthem is a Layer 7c [ModifyStats] over a [GroupFilter] of Heroes you control. No
 *   `excludeSelf` is needed — the enchantment isn't a Hero.
 * - The draw is [Triggers.EachEndStep] (each player's end step, not just yours) with an
 *   intervening-if (CR 603.4): the condition is checked both when the trigger would fire and
 *   again on resolution, which is what "if …" before the effect means.
 * - Both halves are **turn-history** reads, not battlefield-existence checks. That distinction is
 *   the whole card: "you attacked with a Hero this turn" stays true after the Hero trades in
 *   combat, and trading in combat then drawing at end step is the card's ordinary line.
 *   - The attack half is [Conditions.YouAttackedWithCreaturesThisTurn], which walks the player's
 *     own `PlayerAttackersThisTurnComponent` id set. The filter deliberately omits `youControl()`:
 *     the set is already per-player, so scoping is free, and a Hero sitting in the graveyard has
 *     no controller to test — dropping it is what lets a dead attacker still count (its subtype
 *     falls back to the printed type line). Keying off *your* attacker set also kills the
 *     converse bug: gaining control of an opponent's Hero that attacked doesn't qualify.
 *   - The entered half counts entries in `PermanentsEnteredUnderControlThisTurnComponent` via
 *     [DynamicAmounts.subtypeEnteredUnderControlThisTurn], whose records capture subtypes at
 *     entry time — so it survives the Hero leaving or later losing the type, exactly as printed.
 */
val AvengersAssemble = card("Avengers Assemble!") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "Heroes you control get +2/+2.\n" +
        "At the beginning of each end step, if you attacked with a Hero this turn or a Hero " +
        "entered the battlefield under your control this turn, draw a card."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HERO).youControl()),
        )
    }

    triggeredAbility {
        trigger = Triggers.EachEndStep
        triggerCondition = Conditions.Any(
            Conditions.YouAttackedWithCreaturesThisTurn(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.HERO),
                atLeast = 1,
            ),
            Conditions.CompareAmounts(
                DynamicAmounts.subtypeEnteredUnderControlThisTurn(Subtype.HERO),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(1),
            ),
        )
        effect = Effects.DrawCards(1)
        description = "At the beginning of each end step, if you attacked with a Hero this turn " +
            "or a Hero entered the battlefield under your control this turn, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf736399-af74-4f52-9159-67ea67d0cf83.jpg?1783902981"
    }
}
