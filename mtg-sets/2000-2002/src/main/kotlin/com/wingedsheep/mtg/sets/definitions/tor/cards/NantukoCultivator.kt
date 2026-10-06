package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nantuko Cultivator — Torment #133
 * {3}{G} · Creature — Insect Druid · 2/2
 *
 * When this creature enters, you may discard any number of land cards. Put that many +1/+1
 * counters on this creature and draw that many cards.
 *
 * "You may discard any number" is a zero-allowed selection over the land cards in hand
 * ([Patterns.Hand.discardAnyNumber] with a land filter); the discarded collection's size is the
 * "that many" read by both the counters and the draw.
 */
val NantukoCultivator = card("Nantuko Cultivator") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Druid"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may discard any number of land cards. Put that " +
        "many +1/+1 counters on this creature and draw that many cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val discarded = runStoringCollection {
                Patterns.Hand.discardAnyNumber(
                    filter = GameObjectFilter.Land,
                    storeAs = it,
                    prompt = "Choose any number of land cards to discard",
                )
            }
            run(Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, discarded.count, EffectTarget.Self))
            run(Effects.DrawCards(discarded.count))
        }
        description = "When this creature enters, you may discard any number of land cards. Put " +
            "that many +1/+1 counters on this creature and draw that many cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d258fe7-7906-43ca-8ebd-344aa81cb85b.jpg?1783945141"
    }
}
