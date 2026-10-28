package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Data, New Life
 * {1}{U}
 * Legendary Artifact Creature — Android Scientist
 * 0/1
 *
 * Whenever you draw a card, put a +1/+1 counter on Data.
 */
val DataNewLife = card("Data, New Life") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Artifact Creature — Android Scientist"
    colorIdentity = "U"
    oracleText = "Whenever you draw a card, put a +1/+1 counter on Data."
    power = 0
    toughness = 1
    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Kieran Yanner"
        flavorText = "\"Does Data have a soul? I don't know that he has. I don't know that I have. But I have got to give him the freedom to explore that question himself.\"\n—Captain Phillipa Louvois"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96b7e757-c64d-48c8-bd9c-caad5f3c78fb.jpg?1792753877"
    }
}
