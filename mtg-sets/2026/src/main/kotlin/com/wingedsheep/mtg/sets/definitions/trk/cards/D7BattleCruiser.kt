package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * D-7 Battle Cruiser
 * {1}{R}
 * Artifact — Spacecraft
 * 3/3
 *
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 3+ | At the beginning of combat on your turn, create a 2/1 red Klingon Warrior creature token
 *      with haste. Sacrifice it at the beginning of the next end step.
 * 8+ | Flying
 *
 * The 3+ trigger is the Synthesizer Labship shape (begin-combat trigger gated by a charge-counter
 * trigger restriction); the token is the Harried Dronesmith shape (`sacrificeAtStep = Step.END`).
 */
val D7BattleCruiser = card("D-7 Battle Cruiser") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Spacecraft"
    power = 3
    toughness = 3
    oracleText = "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n" +
        "3+ | At the beginning of combat on your turn, create a 2/1 red Klingon Warrior creature token " +
        "with haste. Sacrifice it at the beginning of the next end step.\n" +
        "8+ | Flying"

    station()

    val charge3 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 3)
    val charge8 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)

    // 3+ | At the beginning of combat on your turn, create a hasty 2/1 Klingon Warrior that is
    // sacrificed at the beginning of the next end step.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        triggerRestriction = charge3
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Klingon", "Warrior"),
            keywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7897f31-509e-4099-b9b4-c6214b80f29d.jpg?1793364577",
        )
    }

    // 8+ | It's an artifact creature at 8+ (station reminder text)
    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // 8+ | Flying
    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Kevin Glint"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c22998d-7bd7-433d-95b8-50f2fd8b7b7f.jpg?1792508232"
    }
}
