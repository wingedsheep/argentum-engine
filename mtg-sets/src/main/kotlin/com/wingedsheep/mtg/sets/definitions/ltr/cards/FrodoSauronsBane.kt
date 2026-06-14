package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frodo, Sauron's Bane
 * {W}
 * Legendary Creature — Halfling Citizen
 * 1/2
 *
 * {W/B}{W/B}: If Frodo is a Citizen, it becomes a Halfling Scout with base power and
 * toughness 2/3 and lifelink.
 * {B}{B}{B}: If Frodo is a Scout, it becomes a Halfling Rogue with "Whenever this creature
 * deals combat damage to a player, that player loses the game if the Ring has tempted you
 * four or more times this game. Otherwise, the Ring tempts you."
 *
 * Like Figure of Fable, each ability checks Frodo's creature types on resolution: the
 * conditional gate lives inside the effect, not as an activation restriction, so the ability
 * is always legal to activate but does nothing if Frodo isn't currently the prerequisite form.
 * None of these abilities has a duration — they last until the game ends, Frodo leaves the
 * battlefield, or a later effect changes his characteristics. The Scout step sets base P/T 2/3;
 * the Rogue step keeps that base P/T (it only swaps the class subtype and grants the ability),
 * because "becomes a Halfling Rogue" states no new power/toughness.
 */
/**
 * Granted to Frodo when he becomes a Rogue: "Whenever this creature deals combat damage to a
 * player, that player loses the game if the Ring has tempted you four or more times this game.
 * Otherwise, the Ring tempts you."
 */
private val rogueCombatDamageAbility = TriggeredAbility.create(
    trigger = Triggers.DealsCombatDamageToPlayer.event,
    binding = Triggers.DealsCombatDamageToPlayer.binding,
    effect = ConditionalEffect(
        condition = Conditions.RingHasTemptedYouAtLeast(4),
        effect = Effects.LoseGame(
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            message = "Frodo, Sauron's Bane dealt combat damage (Ring tempted you 4+ times)"
        ),
        elseEffect = Effects.TheRingTemptsYou()
    ),
    descriptionOverride = "Whenever this creature deals combat damage to a player, that player loses the game if the Ring has tempted you four or more times this game. Otherwise, the Ring tempts you."
)

val FrodoSauronsBane = card("Frodo, Sauron's Bane") {
    manaCost = "{W}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Halfling Citizen"
    power = 1
    toughness = 2
    oracleText = "{W/B}{W/B}: If Frodo is a Citizen, it becomes a Halfling Scout with base power and toughness 2/3 and lifelink.\n" +
        "{B}{B}{B}: If Frodo is a Scout, it becomes a Halfling Rogue with \"Whenever this creature deals combat damage to a player, that player loses the game if the Ring has tempted you four or more times this game. Otherwise, the Ring tempts you.\""

    // {W/B}{W/B}: If Frodo is a Citizen, becomes a 2/3 Halfling Scout with lifelink.
    activatedAbility {
        cost = Costs.Mana("{W/B}{W/B}")
        effect = ConditionalEffect(
            condition = Conditions.SourceHasSubtype(Subtype.CITIZEN),
            effect = Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 2,
                toughness = 3,
                keywords = setOf(Keyword.LIFELINK),
                creatureTypes = setOf("Halfling", "Scout"),
                duration = Duration.Permanent
            )
        )
    }

    // {B}{B}{B}: If Frodo is a Scout, becomes a Halfling Rogue with the combat-damage trigger.
    // Keeps his current base P/T (2/3 from the Scout step); only the class subtype changes and
    // the triggered ability is granted permanently.
    activatedAbility {
        cost = Costs.Mana("{B}{B}{B}")
        effect = ConditionalEffect(
            condition = Conditions.SourceHasSubtype(Subtype.SCOUT),
            effect = Effects.SetCreatureSubtypes(
                subtypes = setOf("Halfling", "Rogue"),
                target = EffectTarget.Self,
                duration = Duration.Permanent
            ) then GrantTriggeredAbilityEffect(
                ability = rogueCombatDamageAbility,
                target = EffectTarget.Self,
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d86dc2e-6f0c-4714-9d30-5d099d3b895c.jpg?1686967812"
    }
}
