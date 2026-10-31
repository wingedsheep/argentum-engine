package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pouncing Shoreshark
 * {4}{U}
 * Creature — Shark Beast
 * 4/3
 * Mutate {3}{U}
 * Flash
 * Whenever this creature mutates, you may return target creature an opponent controls to its owner's hand.
 */
val PouncingShoreshark = card("Pouncing Shoreshark") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shark Beast"
    power = 4
    toughness = 3
    oracleText = "Mutate {3}{U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flash\n" +
        "Whenever this creature mutates, you may return target creature an opponent controls to its owner's hand."

    keywordAbility(KeywordAbility.mutate("{3}{U}"))
    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        optional = true
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c859b339-b55b-41fe-948c-27502e3b3ea8.jpg"
    }
}
