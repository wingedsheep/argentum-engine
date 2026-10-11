package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ishai, Ojutai Dragonspeaker
 * {2}{W}{U}
 * Legendary Creature — Bird Monk
 * 1/1
 * Flying
 * Whenever an opponent casts a spell, put a +1/+1 counter on Ishai.
 * Partner (You can have two commanders if both have partner.)
 *
 * Partner is omitted, as on Kodama of the East Tree: the engine does not model
 * multi-commander decks, and Partner has no effect during play.
 */
val IshaiOjutaiDragonspeaker = card("Ishai, Ojutai Dragonspeaker") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Bird Monk"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever an opponent casts a spell, put a +1/+1 counter on Ishai.\n" +
        "Partner (You can have two commanders if both have partner.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "33"
        artist = "Zack Stella"
        flavorText = "\"Through me, the Great Teacher's voice will rattle this world.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2f04b83-da9d-4bb1-99ce-2ff78503c635.jpg?1783937085"
    }
}
