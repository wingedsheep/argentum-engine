package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace, Ingenious Mind-Mage
 * {4}{U}{U}
 * Legendary Planeswalker — Jace
 * Starting Loyalty: 5
 *
 * +1: Draw a card.
 * +1: Untap all creatures you control.
 * −9: Gain control of up to three target creatures.
 *
 * The −9 is one "up to three" target declaration walked by [Effects.ForEachTargetOf]; each stolen
 * creature is a permanent control change (ruling: it doesn't end when you lose control of Jace).
 */
val JaceIngeniousMindMage = card("Jace, Ingenious Mind-Mage") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 5
    oracleText = "+1: Draw a card.\n" +
        "+1: Untap all creatures you control.\n" +
        "−9: Gain control of up to three target creatures."

    // +1: Draw a card.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1)
    }

    // +1: Untap all creatures you control.
    loyaltyAbility(+1) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    // −9: Gain control of up to three target creatures.
    loyaltyAbility(-9) {
        val creatures = targets(TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.ForEachTargetOf(creatures, Effects.GainControl(EffectTarget.IterationEntity))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "280"
        inBooster = false
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3b274f6-833f-430f-ac1a-d1d1d5b03e71.jpg?1783935685"
        ruling(
            "2017-09-29",
            "The control-change effect of Jace's last ability lasts indefinitely. It doesn't wear off during " +
                "the cleanup step or when you lose control of Jace.",
        )
        ruling(
            "2017-09-29",
            "In a multiplayer game, if a player leaves the game, all cards that player owns leave as well, " +
                "and any effects that give the player control of permanents immediately end.",
        )
    }
}
