package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenhill Flock
 * {3}{U}
 * Creature — Bird
 * 1/2
 * Flying
 * Whenever you draw a card, put a +1/+1 counter on this creature.
 *
 * `YouDraw` fires once per individual card drawn (CR 121.2), so "draw two cards" puts on two
 * counters — which is what the card wants.
 */
val RavenhillFlock = card("Ravenhill Flock") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    oracleText = "Flying\nWhenever you draw a card, put a +1/+1 counter on this creature."
    power = 1
    toughness = 2
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.YouDraw
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Irina Nordsol"
        flavorText = "\"They live many a year, and their memories are long, and they hand on their wisdom to their children. I knew many among the ravens of the rocks when I was a Dwarf-lad.\"\n—Balin"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acbb4d32-2771-469e-a6de-0df15155cc62.jpg?1784714603"
    }
}
