package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glint-Horn Buccaneer
 * {1}{R}{R}
 * Creature — Minotaur Pirate
 * 2/4
 * Haste
 * Whenever you discard a card, this creature deals 1 damage to each opponent.
 * {1}{R}, Discard a card: Draw a card. Activate only if this creature is attacking.
 */
val GlintHornBuccaneer = card("Glint-Horn Buccaneer") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Pirate"
    oracleText = "Haste\n" +
        "Whenever you discard a card, this creature deals 1 damage to each opponent.\n" +
        "{1}{R}, Discard a card: Draw a card. Activate only if this creature is attacking."
    power = 2
    toughness = 4

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever you discard a card, this creature deals 1 damage to each opponent."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardCard)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.SourceIsAttacking))
        effect = Effects.DrawCards(1)
        description = "{1}{R}, Discard a card: Draw a card. Activate only if this creature is attacking."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df2df9cb-14f5-470f-b438-20f4ae8d0d59.jpg?1783932979"
        ruling("2019-07-12", "The middle ability of Glint-Horn Buccaneer is a triggered ability, not an activated ability. It doesn't allow you to discard a card whenever you want; rather, you need some other way of discarding a card, such as by activating its last ability.")
        ruling("2019-07-12", "In a Two-Headed Giant game, Glint-Horn Buccaneer's triggered ability causes the opposing team to be dealt 1 damage twice.")
    }
}
