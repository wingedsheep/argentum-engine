package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skybeast Tracker
 * {3}{G}
 * Creature — Giant Archer
 * 2/4
 *
 * Reach
 * Whenever you cast a spell with mana value 5 or greater, create a Food token.
 *
 * Same cast trigger as Up the Beanstalk — it fires on *cast*, not on resolution, so the Food is
 * created even if the spell is later countered. For a spell with {X} in its cost, the value chosen
 * for X counts toward mana value (per the card's ruling), which is what
 * `GameObjectFilter.Any.manaValueAtLeast` reads off the spell on the stack.
 */
val SkybeastTracker = card("Skybeast Tracker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Archer"
    oracleText = "Reach\nWhenever you cast a spell with mana value 5 or greater, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"
    power = 2
    toughness = 4

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.youCastSpell(GameObjectFilter.Any.manaValueAtLeast(5))
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Andreas Zafiratos"
        flavorText = "Cloud eaters, heaven tillers, beanstalk wurms—he favors prey worthy of a giant's appetite."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a08da5c6-ebe7-4166-99d5-2aca5b0b529f.jpg?1783915079"
        ruling("2023-09-01", "If a spell has {X} in its mana cost, use the value chosen for that X to determine the mana value of that spell.")
    }
}
