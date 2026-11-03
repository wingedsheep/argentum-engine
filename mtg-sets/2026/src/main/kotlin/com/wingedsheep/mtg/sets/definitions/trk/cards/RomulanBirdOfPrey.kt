package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Romulan Bird-of-Prey
 * {1}{U}
 * Artifact — Spacecraft
 * 4/4
 *
 * Cloaking Device — As long as this Spacecraft isn't a creature, it has hexproof.
 * Whenever you cast a noncreature spell, if this Spacecraft isn't a creature, put two charge
 * counters on it.
 * Station
 * 8+ | Flying
 *
 * - "Isn't a creature" reads the Spacecraft's *projected* types, not its charge-counter count, so
 *   anything else that turns it into a creature (an animate effect) also drops the hexproof and
 *   switches the counter trigger off — and anything that strips its creature type turns them back on.
 * - The trigger's "if" is an intervening-if (CR 603.4): checked when the spell is cast and again
 *   as the trigger resolves.
 */
val RomulanBirdOfPrey = card("Romulan Bird-of-Prey") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Spacecraft"
    power = 4
    toughness = 4
    oracleText = "Cloaking Device — As long as this Spacecraft isn't a creature, it has hexproof.\n" +
        "Whenever you cast a noncreature spell, if this Spacecraft isn't a creature, put two charge " +
        "counters on it.\n" +
        "Station\n" +
        "8+ | Flying"

    val notACreature = Conditions.Not(Conditions.SourceMatches(GameObjectFilter.Creature))

    // Cloaking Device
    staticAbility {
        condition = notACreature
        ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        interveningIf = notACreature
        effect = Effects.AddCounters(CounterType.CHARGE, 2, EffectTarget.Self)
        description = "Whenever you cast a noncreature spell, if this Spacecraft isn't a creature, " +
            "put two charge counters on it."
    }

    station()

    val charge8 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Edouard"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a461d90-6b7b-4777-958a-8a5d8365ad8c.jpg?1792508202"
    }
}
