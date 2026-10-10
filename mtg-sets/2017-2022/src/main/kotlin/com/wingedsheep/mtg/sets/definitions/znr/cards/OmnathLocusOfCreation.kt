package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Omnath, Locus of Creation — Zendikar Rising #232
 * {R}{G}{W}{U} · Legendary Creature — Elemental · 4/4
 *
 * When Omnath enters, draw a card.
 * Landfall — Whenever a land you control enters, you gain 4 life if this is the first time this
 * ability has resolved this turn. If it's the second time, add {R}{G}{W}{U}. If it's the third time,
 * Omnath deals 4 damage to each opponent and each planeswalker you don't control.
 *
 * The landfall payoff bumps the source's per-turn resolution counter and branches on the exact count
 * (cf. Victor, Valgavoth's Seneschal); the fourth and later resolutions do nothing. The ETB draw does
 * not touch the counter. "Each planeswalker you don't control" is `Not(ControlledByYou)` rather than
 * "an opponent controls", so a Two-Headed Giant teammate's planeswalkers are hit too (the card's ruling).
 */
val OmnathLocusOfCreation = card("Omnath, Locus of Creation") {
    manaCost = "{R}{G}{W}{U}"
    colorIdentity = "RGWU"
    typeLine = "Legendary Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "When Omnath enters, draw a card.\n" +
        "Landfall — Whenever a land you control enters, you gain 4 life if this is the first time this " +
        "ability has resolved this turn. If it's the second time, add {R}{G}{W}{U}. If it's the third " +
        "time, Omnath deals 4 damage to each opponent and each planeswalker you don't control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(1),
                then = Effects.GainLife(4),
            ) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.AddMana(Color.RED) then
                    Effects.AddMana(Color.GREEN) then
                    Effects.AddMana(Color.WHITE) then
                    Effects.AddMana(Color.BLUE),
            ) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(3),
                then = Effects.DealDamage(4, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Patterns.Group.dealDamageToAll(
                        4,
                        GroupFilter(
                            GameObjectFilter.Planeswalker.withControllerPredicate(
                                ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                            )
                        )
                    ),
            )
        description = "Landfall — Whenever a land you control enters, you gain 4 life if this is the " +
            "first time this ability has resolved this turn. If it's the second time, add {R}{G}{W}{U}. " +
            "If it's the third time, Omnath deals 4 damage to each opponent and each planeswalker you " +
            "don't control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "232"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e4fb50c-a81f-44d3-93c5-fa9a0b37f617.jpg?1783929320"
        ruling("2020-09-25", "Omnath's landfall ability has no effect each time beyond the third it resolves in a turn.")
        ruling("2020-09-25", "Omnath's landfall ability always uses the stack and players may respond to it. It isn't a mana ability because the event that causes it to trigger isn't a mana ability, even if it's the second time the ability has resolved in a turn.")
        ruling("2020-09-25", "In a Two-Headed Giant game, Omnath's second ability causes the opposing team to lose 8 life the third time it resolves. Each other player's planeswalkers—including your teammate's—are dealt 4 damage.")
    }
}
