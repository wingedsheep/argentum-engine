package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cold-Blooded Crew
 * {4}{G}{G}
 * Creature — Gorn Warrior
 * 4/4
 *
 * When this creature enters, put two +1/+1 counters on target creature.
 * This creature can't be blocked by creatures with power 2 or less.
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 */
val ColdBloodedCrew = card("Cold-Blooded Crew") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Gorn Warrior"
    colorIdentity = "G"
    oracleText = "When this creature enters, put two +1/+1 counters on target creature.\nThis creature can't be blocked by creatures with power 2 or less.\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 4
    toughness = 4
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }
    staticAbility { ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)) }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "John Tyler Christopher"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f2d1830-baea-4735-b029-cb8c0b538d83.jpg?1785981563"
    }
}
