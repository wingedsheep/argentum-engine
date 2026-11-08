package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Territorial Gorger — Kaladesh #136
 * {3}{R} · Creature — Gremlin · 2/2
 *
 * Trample
 * Whenever you get one or more {E} (energy counters), this creature gets +2/+2 until end of turn.
 *
 * The energy trigger is the player-keyed `Triggers.you.getsCounters` (Brotherhood Scribe shape):
 * one grant of several {E} fires it once.
 */
val TerritorialGorger = card("Territorial Gorger") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Gremlin"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever you get one or more {E} (energy counters), this creature gets +2/+2 until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.getsCounters(CounterType.ENERGY)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Lius Lasahido"
        flavorText = "Inventors have been known to capture gremlins with the intent to release them in a " +
            "rival's workshop. More often than not, that plan backfires."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3525ad1-3c17-4959-ab61-ae0e784c526a.jpg?1783937186"
        ruling(
            "2016-09-20",
            "If you get multiple {E} at once, Territorial Gorger only gets +2/+2 once, not +2/+2 per {E}. " +
                "If you get multiple {E} at different times, it triggers once each time."
        )
    }
}
