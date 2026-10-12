package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenous Gigamole — The Brothers' War #113
 * {3}{B} · Creature — Mole Horror · 2/3
 *
 * When this creature enters, mill three cards. You may put a creature card from among the
 * cards milled this way into your hand. If you don't, put a +1/+1 counter on this creature.
 *
 * Same shape as Ostrich-Horse / Ainok Wayfarer: gather the top three, mill them, then choose
 * up to one creature card among them for hand; an empty choice (declined, or no creature
 * milled) puts the +1/+1 counter on this creature instead.
 */
val RavenousGigamole = card("Ravenous Gigamole") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Mole Horror"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, mill three cards. You may put a creature card from among " +
        "the cards milled this way into your hand. If you don't, put a +1/+1 counter on this creature. " +
        "(To mill a card, put the top card of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = mill(3)
            val creatures = filter(milled, GameObjectFilter.Creature)
            val chosen = chooseUpTo(1, from = creatures)
            ifNotEmpty(chosen) {
                toHand(chosen)
            } orElse {
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fee3328-75c2-4d13-86e1-bf3a40fbf538.jpg?1783920081"
    }
}
