package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fiend Binder — Eldritch Moon #26
 * {3}{W} · Creature — Human Soldier · 3/2
 *
 * Whenever this creature attacks, tap target creature defending player controls.
 *
 * Same attack-trigger shape as Star-Crowned Stag (M19).
 */
val FiendBinder = card("Fiend Binder") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Whenever this creature attacks, tap target creature defending player controls."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "David Gaillet"
        flavorText = "She had not believed any shred of humanity remained in these writhing monsters until the twisted form of her partner appeared before her."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0870d21-603f-4d36-8054-943adba06301.jpg?1783937518"
    }
}
