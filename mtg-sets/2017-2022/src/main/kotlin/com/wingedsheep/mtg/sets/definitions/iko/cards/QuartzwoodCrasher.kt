package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Quartzwood Crasher — Ikoria: Lair of Behemoths #201 (canonical printing)
 * {2}{R}{R}{G} · Creature — Dinosaur Beast · 6/6
 *
 * Trample
 * Whenever one or more creatures you control with trample deal combat damage to a player, create
 * an X/X green Dinosaur Beast creature token with trample, where X is the amount of damage those
 * creatures dealt to that player.
 *
 * The batched combat-damage trigger fires once per player hit and sums the damage the matching
 * tramplers dealt that player in the step into `triggerDamageAmount()`; the token's size is
 * fixed from it at creation.
 */
val QuartzwoodCrasher = card("Quartzwood Crasher") {
    manaCost = "{2}{R}{R}{G}"
    typeLine = "Creature — Dinosaur Beast"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Whenever one or more creatures you control with trample deal combat damage to a player, create an " +
        "X/X green Dinosaur Beast creature token with trample, where X is the amount of damage those " +
        "creatures dealt to that player."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withKeyword(Keyword.TRAMPLE))
            .dealCombatDamageToAPlayer()
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.triggerDamageAmount(),
            dynamicToughness = DynamicAmounts.triggerDamageAmount(),
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Dinosaur", "Beast"),
            keywords = setOf(Keyword.TRAMPLE),
            imageUri = "https://cards.scryfall.io/normal/front/9/d/9d447365-3aca-4996-9098-efcbe6f28f57.jpg?1783930943"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8e4c609-19c9-433b-a852-7999e375ee4f.jpg?1783931019"
        ruling("2020-04-17", "Quartzwood Crasher's second ability includes damage dealt by Quartzwood Crasher itself as long as it still has trample.")
        ruling("2020-04-17", "If a creature you control with trample is unblocked, the full amount of combat damage it deals to the defending player is counted by Quartzwood Crasher's last ability.")
    }
}
