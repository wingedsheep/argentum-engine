package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nimble Trapfinder — Zendikar Rising #72
 * {1}{U} · Creature — Human Rogue · 2/1
 *
 * This creature can't be blocked if you had another Cleric, Rogue, Warrior, or Wizard enter the
 * battlefield under your control this turn.
 * At the beginning of combat on your turn, if you have a full party, creatures you control gain
 * "Whenever this creature deals combat damage to a player, draw a card" until end of turn.
 *
 * - The unblockable clause reads the per-player entry log
 *   ([DynamicAmounts.subtypesEnteredUnderControlThisTurn]), whose records keep the subtypes each
 *   permanent had as it entered — so a party creature that has since left still counts, and one
 *   that only gained a party type later does not (2020-09-25 ruling). That log also holds this
 *   Rogue's own entry when it entered under your control this turn, so "another" is one entry if
 *   it didn't and two if it did (the Swordsworn Cavalier shape).
 * - The combat trigger is a one-shot grant over the creatures you control at resolution
 *   (CR 611.2c): creatures arriving later don't get the ability, and the granted ability stays
 *   for the turn even if the party breaks afterwards.
 */
val NimbleTrapfinder = card("Nimble Trapfinder") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "This creature can't be blocked if you had another Cleric, Rogue, Warrior, or Wizard " +
        "enter the battlefield under your control this turn.\n" +
        "At the beginning of combat on your turn, if you have a full party, creatures you control gain " +
        "\"Whenever this creature deals combat damage to a player, draw a card\" until end of turn."

    val partyEntries = DynamicAmounts.subtypesEnteredUnderControlThisTurn(
        setOf(Subtype.CLERIC, Subtype.ROGUE, Subtype.WARRIOR, Subtype.WIZARD)
    )
    val anotherPartyCreatureEntered = Conditions.Any(
        Conditions.All(
            Conditions.Not(Conditions.SourceMatches(GameObjectFilter.Any.enteredThisTurnUnderYourControl())),
            Conditions.CompareAmounts(partyEntries, ComparisonOperator.GTE, 1),
        ),
        Conditions.CompareAmounts(partyEntries, ComparisonOperator.GTE, 2),
    )

    staticAbility {
        ability = CantBeBlocked(filter = GroupFilter.source())
        condition = anotherPartyCreatureEntered
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.FullParty()
        val combatDamageDraw = TriggeredAbility.create(
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
            effect = Effects.DrawCards(1),
            descriptionOverride = "Whenever this creature deals combat damage to a player, draw a card.",
        )
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            effect = Effects.GrantTriggeredAbility(
                ability = combatDamageDraw,
                target = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee1a550-b7e7-48e0-bd96-a41986dbd879.jpg?1783929391"

        ruling("2020-09-25", "Nimble Trapfinder's first ability checks only whether a permanent entered the battlefield under your control earlier in the turn and had one of the party creature types as it entered. It doesn't apply if an entering object gained the type later in the turn. It does apply if the entering object left your control since. It doesn't matter whether Nimble Trapfinder was under your control as that object entered.")
        ruling("2020-09-25", "Nimble Trapfinder's second ability affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't get the triggered ability.")
        ruling("2020-09-25", "After Nimble Trapfinder's second ability has resolved, creatures you control keep the triggered ability for the turn even if you no longer have a full party later in the turn.")
    }
}
