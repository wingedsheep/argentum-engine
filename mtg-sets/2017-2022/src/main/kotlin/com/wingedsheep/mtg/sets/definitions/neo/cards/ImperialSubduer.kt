package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Imperial Subduer — Kamigawa: Neon Dynasty #19 (canonical printing)
 * {2}{W} · Creature — Human Samurai · 3/2
 *
 * Whenever a Samurai or Warrior you control attacks alone, tap target creature you don't control.
 */
val ImperialSubduer = card("Imperial Subduer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Samurai"
    power = 3
    toughness = 2
    oracleText = "Whenever a Samurai or Warrior you control attacks alone, tap target creature you don't control."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(victim)
        description = "Whenever a Samurai or Warrior you control attacks alone, tap target creature you don't control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Zara Alfonso"
        flavorText = "\"Cease your pathetic struggling.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13246172-7554-4edc-b6db-23dc91e4290a.jpg?1783923920"
    }
}
