package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Star-Crowned Stag — Core Set 2019 #38
 * {3}{W} · Creature — Elk · 3/3
 *
 * Whenever this creature attacks, tap target creature defending player controls.
 *
 * Hellkite Whelp's attack-trigger shape: `TargetFilter.Creature.defendingPlayerControls()` + `Effects.Tap`.
 */
val StarCrownedStag = card("Star-Crowned Stag") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elk"
    oracleText = "Whenever this creature attacks, tap target creature defending player controls."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Jonathan Kuo"
        flavorText = "White as starlight on snow, it appears only on the eve of the winter solstice."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c57417c7-867b-4b64-bfe3-1744dfe9b44d.jpg?1783934597"
    }
}
