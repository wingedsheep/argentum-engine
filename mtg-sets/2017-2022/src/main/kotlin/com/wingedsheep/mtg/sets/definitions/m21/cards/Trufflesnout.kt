package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trufflesnout — Core Set 2021 #212 (only printing)
 * {2}{G} · Creature — Boar · 2/2
 *
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • You gain 4 life.
 */
val Trufflesnout = card("Trufflesnout") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Boar"
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• You gain 4 life."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            Mode.noTarget(
                Effects.GainLife(4),
                "You gain 4 life",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "212"
        artist = "Jason Kang"
        flavorText = "Its exploits are the stuff of legends among local chefs and fuel for nightmares among the forest thallids."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bba47c1-a874-456e-bea3-e99e2d61cfba.jpg?1783930665"
    }
}
