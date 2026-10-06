package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skullslither Worm
 * {3}{B}
 * Creature — Worm
 * 3/3
 * When this creature enters, each opponent discards a card. For each opponent who can't, put two
 * +1/+1 counters on this creature.
 *
 * An opponent "can't" discard exactly when their hand is empty, so the opponents with no cards in
 * hand are recorded first (`storePlayer(onlyIf = EmptyHand)` per opponent), then every opponent
 * discards, then the Worm gets two counters per recorded opponent.
 */
val SkullslitherWorm = card("Skullslither Worm") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Worm"
    oracleText = "When this creature enters, each opponent discards a card. For each opponent who can't, " +
        "put two +1/+1 counters on this creature."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val cant = forEachPlayerCollecting(Player.EachOpponent) {
                listOf(storePlayer(onlyIf = Conditions.EmptyHand))
            }.single()
            run(Effects.EachOpponentDiscards(1))
            run(Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, cant.count * 2, EffectTarget.Self))
        }
        description = "When this creature enters, each opponent discards a card. For each opponent who can't, " +
            "put two +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Uriah Voth"
        flavorText = "As larvae, they burrow into a host's brain to feed upon it, growing until they're " +
            "large enough to burst free and seek out larger prey."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fdd4db9-8cab-4df2-ae65-0d7546c2d06b.jpg?1783919188"
    }
}
