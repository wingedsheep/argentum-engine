package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Voice of the Vermin
 * {3}{G}
 * Creature — Human Citizen
 * 2 / 2
 * This creature enters with a shield counter on it.
 * Whenever this creature attacks, target creature you control has base power and toughness 4/4
 * until end of turn.
 *
 * "Base power and toughness 4/4" is a layer 7b set, so counters and pumps still apply on top.
 */
val VoiceOfTheVermin = card("Voice of the Vermin") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen"
    oracleText = "This creature enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)\n" +
        "Whenever this creature attacks, target creature you control has base power and toughness 4/4 until end of turn."
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.SetBasePowerAndToughness(4, 4, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfe4e0eb-23a5-46a5-b719-5a6f9a1fc4ce.jpg"
    }
}
