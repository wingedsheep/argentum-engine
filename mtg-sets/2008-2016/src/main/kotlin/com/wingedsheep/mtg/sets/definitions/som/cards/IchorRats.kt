package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ichor Rats
 * {1}{B}{B}
 * Creature — Phyrexian Rat
 * 2/1
 *
 * Infect
 * When this creature enters, each player gets a poison counter.
 *
 * "Each player gets" is the Phyresis Outbreak shape over every player: [Effects.ForEachPlayer]
 * rebinds [Player.You] to the player being iterated.
 */
val IchorRats = card("Ichor Rats") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Rat"
    power = 2
    toughness = 1
    oracleText = "Infect (This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters.)\n" +
        "When this creature enters, each player gets a poison counter."

    keywords(Keyword.INFECT)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2013aed6-7415-4bf0-a3bb-46d6beecbaff.jpg?1783941730"
    }
}
