package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flawless Maneuver — Commander 2020 #26 (canonical printing)
 * {2}{W} · Instant
 *
 * If you control a commander, you may cast this spell without paying its mana cost.
 * Creatures you control gain indestructible until end of turn.
 *
 * The free cast is Fierce Guardianship's shape: a `{0}` [SelfAlternativeCost] gated on
 * [Conditions.YouControlACommander]. The grant locks in the creatures you control as it resolves.
 */
val FlawlessManeuver = card("Flawless Maneuver") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "If you control a commander, you may cast this spell without paying its mana cost.\n" +
        "Creatures you control gain indestructible until end of turn."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        condition = Conditions.YouControlACommander
    )

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Zoltan Boros"
        flavorText = "\"Captain, your units are the tip of our spear. There is zero room for failure.\"\n—Jirina Kudro"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c972abe6-c732-4745-bde4-8b51698f05be.jpg?1783931225"
        ruling(
            "2020-04-17",
            "It doesn't matter whose commander you control. Any one will do. If you have two commanders, " +
                "you just need to control one of them."
        )
        ruling(
            "2020-04-17",
            "Once you begin casting this spell, players can't take any other actions until you're done casting " +
                "it. Notably, they can't try to remove the commander you control to make you pay its cost."
        )
    }
}
