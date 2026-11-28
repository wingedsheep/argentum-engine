package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Increasing Savagery — Dark Ascension #120 (canonical printing)
 * {2}{G}{G} · Sorcery
 *
 * Put five +1/+1 counters on target creature. If this spell was cast from a graveyard, put ten +1/+1
 * counters on that creature instead.
 * Flashback {5}{G}{G}
 *
 * The cycle's Increasing Ambition / Vengeance shape: a resolution-time cast-from-graveyard check picks
 * between five counters and ten.
 */
val IncreasingSavagery = card("Increasing Savagery") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put five +1/+1 counters on target creature. If this spell was cast from a graveyard, " +
        "put ten +1/+1 counters on that creature instead.\n" +
        "Flashback {5}{G}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 10, creature),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 5, creature),
        )
    }

    keywordAbility(KeywordAbility.flashback("{5}{G}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29ab8737-151f-4702-a95d-7f7b60a5ee8a.jpg?1783940804"
    }
}
