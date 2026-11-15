package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goblin Heelcutter
 * {3}{R}
 * Creature — Goblin Berserker
 * 3/2
 * Whenever this creature attacks, target creature can't block this turn.
 * Dash {2}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)
 */
val GoblinHeelcutter = card("Goblin Heelcutter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, target creature can't block this turn.\n" +
        "Dash {2}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    dash = "{2}{R}"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b3bfeb6-95d6-4f00-8981-c6d3c9c93f67.jpg"
    }
}
