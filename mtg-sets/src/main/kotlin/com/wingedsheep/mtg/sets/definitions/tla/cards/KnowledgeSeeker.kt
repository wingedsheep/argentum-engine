package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Knowledge Seeker
 * {1}{U}
 * Creature — Fox Spirit
 * 2/1
 *
 * Vigilance
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 * When this creature dies, create a Clue token. (It's an artifact with
 * "{2}, Sacrifice this token: Draw a card.")
 */
val KnowledgeSeeker = card("Knowledge Seeker") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fox Spirit"
    power = 2
    toughness = 1
    oracleText = "Vigilance\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, create a Clue token. (It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you draw your second card each turn, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateClue()
        description = "When this creature dies, create a Clue token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Shiren"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/8554e862-f78a-42f5-b876-076aac6c9504.jpg?1764120328"
    }
}
