package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lifebane Zombie
 * {1}{B}{B}
 * Creature — Zombie Warrior
 * 3/1
 *
 * Intimidate
 * When this creature enters, target opponent reveals their hand. You choose a green or white
 * creature card from it and exile that card.
 *
 * Canonical M14 printing (its first). The ETB is Thought-Knot Seer's
 * `Patterns.Hand.revealHandAndExileChosen`, filtered to green-or-white creature cards.
 */
val LifebaneZombie = card("Lifebane Zombie") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warrior"
    power = 3
    toughness = 1
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or " +
        "creatures that share a color with it.)\n" +
        "When this creature enters, target opponent reveals their hand. You choose a green or " +
        "white creature card from it and exile that card."

    keywords(Keyword.INTIMIDATE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.revealHandAndExileChosen(
            target = opponent,
            filter = GameObjectFilter.Creature.withAnyColor(Color.GREEN, Color.WHITE),
            prompt = "Choose a green or white creature card to exile",
        )
        description = "When this creature enters, target opponent reveals their hand. You " +
            "choose a green or white creature card from it and exile that card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98370735-5303-40d4-9e80-cdb40dee18e2.jpg?1783939922"
    }
}
