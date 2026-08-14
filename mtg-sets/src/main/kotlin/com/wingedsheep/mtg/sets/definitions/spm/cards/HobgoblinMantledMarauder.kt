package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hobgoblin, Mantled Marauder
 * {1}{R}
 * Legendary Creature — Goblin Human Villain
 * 1/2
 * Flying, haste
 * Whenever you discard a card, Hobgoblin gets +2/+0 until end of turn.
 */
val HobgoblinMantledMarauder = card("Hobgoblin, Mantled Marauder") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Human Villain"
    power = 1
    toughness = 2
    oracleText = "Flying, haste\n" +
        "Whenever you discard a card, Hobgoblin gets +2/+0 until end of turn."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.YouDiscard
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you discard a card, Hobgoblin gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Dave DeVries"
        flavorText = "\"Alrighty, let's see who's behind the mask this time. Kingsley, is that you?\"\n—Spider-Man"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50716fe3-7a19-431e-8758-984fc48d714e.jpg?1783905338"
    }
}
