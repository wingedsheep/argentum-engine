package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Master of Diversion — Magic 2014 #24
 * {2}{W} · Creature — Human Scout · 2/2
 *
 * Whenever this creature attacks, tap target creature defending player controls.
 *
 * Same attack-trigger shape as Fiend Binder (EMN).
 */
val MasterOfDiversion = card("Master of Diversion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    oracleText = "Whenever this creature attacks, tap target creature defending player controls."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Michael Komarck"
        flavorText = "Choose your battles carefully. For example, avoid the big guy."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bec89b3-640e-4093-a6e9-5639610769b9.jpg?1783939941"
    }
}
