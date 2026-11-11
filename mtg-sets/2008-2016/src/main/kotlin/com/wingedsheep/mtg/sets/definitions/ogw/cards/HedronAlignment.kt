package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hedron Alignment
 * {2}{U}
 * Enchantment
 * Hexproof
 * At the beginning of your upkeep, you may reveal your hand. If you do, you win the game if you
 * own a card named Hedron Alignment in exile, in your hand, in your graveyard, and on the
 * battlefield.
 * {1}{U}: Scry 1.
 *
 * The win check is not an intervening "if": the ability always triggers, and the four-zone test
 * runs on resolution after the reveal. Hand, graveyard and exile are owner-keyed zones, so
 * `Player.You` reads cards you own there; the battlefield copy is read across every player's
 * permanents filtered by *ownership* ("own", not "control"). The exiled copy must be face up
 * (ruling).
 */
private val namedHedronAlignment = GameObjectFilter.Any.named("Hedron Alignment")

val HedronAlignment = card("Hedron Alignment") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Hexproof\n" +
        "At the beginning of your upkeep, you may reveal your hand. If you do, you win the game if " +
        "you own a card named Hedron Alignment in exile, in your hand, in your graveyard, and on " +
        "the battlefield.\n" +
        "{1}{U}: Scry 1."

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.RevealHand(EffectTarget.Controller) then Effects.If(
                Conditions.All(
                    Exists(Player.You, Zone.EXILE, namedHedronAlignment.faceUp()),
                    Exists(Player.You, Zone.HAND, namedHedronAlignment),
                    Exists(Player.You, Zone.GRAVEYARD, namedHedronAlignment),
                    Exists(Player.Each, Zone.BATTLEFIELD, namedHedronAlignment.ownedByYou()),
                ),
                Effects.WinGame()
            ),
            prompt = "Reveal your hand?"
        )
        description = "At the beginning of your upkeep, you may reveal your hand. If you do, you win " +
            "the game if you own a card named Hedron Alignment in exile, in your hand, in your " +
            "graveyard, and on the battlefield."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b3ec511-1b78-46e9-8ec2-fc09abd1b434.jpg?1783937918"
        ruling("2016-01-22", "The Hedron Alignment in exile must be face up. If it’s face down, it won’t count, even if you are allowed to look at it.")
    }
}
