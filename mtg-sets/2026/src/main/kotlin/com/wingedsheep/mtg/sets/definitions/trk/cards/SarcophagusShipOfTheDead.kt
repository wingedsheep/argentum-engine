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
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarcophagus, Ship of the Dead
 * {2}{R}
 * Legendary Artifact — Spacecraft
 * 4/4
 *
 * Other creatures you control get +1/+0.
 * Whenever a creature dies, if Sarcophagus isn't a creature, put a charge counter on it.
 * Station
 * 10+ | Flying
 *
 * "Isn't a creature" reads the Spacecraft's projected types (as Romulan Bird-of-Prey does) and is
 * an intervening-if, checked when the creature dies and again on resolution.
 */
val SarcophagusShipOfTheDead = card("Sarcophagus, Ship of the Dead") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 4
    toughness = 4
    oracleText = "Other creatures you control get +1/+0.\n" +
        "Whenever a creature dies, if Sarcophagus isn't a creature, put a charge counter on it.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 10+.)\n" +
        "10+ | Flying"

    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter.OtherCreaturesYouControl)
    }

    val notACreature = Conditions.Not(Conditions.SourceMatches(GameObjectFilter.Creature))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        interveningIf = notACreature
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "Whenever a creature dies, if Sarcophagus isn't a creature, put a charge " +
            "counter on it."
    }

    station()

    val charge10 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 10)

    staticAbility {
        condition = charge10
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge10
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Joan Piqué Llorens"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ea32b5-aa58-4f87-ac2e-4e3b8dd4a752.jpg?1793187110"
    }
}
