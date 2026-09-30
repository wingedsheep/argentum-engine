package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kaito, Dancing Shadow — Phyrexia: All Will Be One #204
 * {2}{U}{B} · Legendary Planeswalker — Kaito · Starting loyalty 3
 *
 * Whenever one or more creatures you control deal combat damage to a player, you may return one
 * of them to its owner's hand. If you do, you may activate loyalty abilities of Kaito twice this
 * turn rather than only once.
 * +1: Up to one target creature can't attack or block until your next turn.
 * 0: Draw a card.
 * −2: Create a 2/2 colorless Drone artifact creature token with deathtouch and "When this token
 * leaves the battlefield, each opponent loses 2 life and you gain 2 life."
 *
 * "One of them" is the trigger's captured batch (the creatures that hit that player), still on the
 * battlefield; `IfYouDo` scores the return by the zone move, so declining grants nothing. The
 * allowance is not additive — a second trigger the same turn still means twice.
 */
val KaitoDancingShadow = card("Kaito, Dancing Shadow") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Kaito"
    startingLoyalty = 3
    oracleText = "Whenever one or more creatures you control deal combat damage to a player, you may return one of them to its owner's hand. If you do, you may activate loyalty abilities of Kaito twice this turn rather than only once.\n" +
        "+1: Up to one target creature can't attack or block until your next turn.\n" +
        "0: Draw a card.\n" +
        "−2: Create a 2/2 colorless Drone artifact creature token with deathtouch and \"When this token leaves the battlefield, each opponent loses 2 life and you gain 2 life.\""

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val hitters = filter(triggerCaptured, GameObjectFilter.Any.currentlyIn(Zone.BATTLEFIELD))
                val chosen = chooseUpTo(1, from = hitters, prompt = "You may return one of those creatures to its owner's hand")
                move(chosen, CardDestination.ToZone(Zone.HAND))
            },
            then = Effects.AllowLoyaltyActivationsThisTurn()
        )
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn)
    }

    loyaltyAbility(0) {
        effect = Effects.DrawCards(1)
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Drone"),
            keywords = setOf(Keyword.DEATHTOUCH),
            artifactToken = true,
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.leaves(),
                    effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
                        Effects.GainLife(2),
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/c/f/cfacac5f-7685-4792-9e4c-166d4b421240.jpg?1783918167"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d97f59a-07b8-47ef-aa88-d94f8c3feae4.jpg?1783918002"
        ruling("2023-02-04", "If you return a creature to its owner's hand with Kaito's first ability, you may activate a single loyalty ability of Kaito twice, or you may activate two different loyalty abilities.")
        ruling("2023-02-04", "Even if Kaito's triggered ability triggers more than once in the same turn (perhaps because one or more attackers had first strike, multiple players were dealt combat damage by your creatures, or there were multiple combats), and you return an attacking creature to hand each time, you won't be able to activate Kaito's loyalty abilities more than twice that turn. That is, Kaito's triggered ability is not additive.")
    }
}
