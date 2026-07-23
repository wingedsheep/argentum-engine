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
 * The trigger fires on cast, so it uses the spell's mana value *on the stack* — X counts as
 * the chosen value, and a cost reduction doesn't lower it (CR 202.3, 601.2b).
 */
val SkybeastTracker = card("Skybeast Tracker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Archer"
    oracleText = "Reach\n" +
        "Whenever you cast a spell with mana value 5 or greater, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"
    power = 2
    toughness = 4

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.manaValueAtLeast(5))
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Andreas Zafiratos"
        flavorText = "Cloud eaters, heaven tillers, beanstalk wurms—he favors prey worthy of a giant's appetite."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a08da5c6-ebe7-4166-99d5-2aca5b0b529f.jpg?1783915079"
    }
}
