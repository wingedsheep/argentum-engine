package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dauntless Survivor — Zendikar Rising #184
 * {1}{G} · Creature — Human Warrior · 1/1
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 */
val DauntlessSurvivor = card("Dauntless Survivor") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, put a +1/+1 counter on target creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "David Rapoza"
        flavorText = "\"I've got a little surprise for you, beasties! Come and get it!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52b1b4a2-52a7-4fcd-aa96-f7d9e459657b.jpg"
    }
}
