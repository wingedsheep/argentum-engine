package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Rootless Yew
 * {3}{G}{G}
 * Creature — Treefolk
 * 5/4
 * When this creature dies, search your library for a creature card with power or toughness
 * 6 or greater, reveal it, put it into your hand, then shuffle.
 */
val RootlessYew = card("Rootless Yew") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 5
    toughness = 4
    oracleText = "When this creature dies, search your library for a creature card with power or toughness 6 or greater, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.powerOrToughnessAtLeast(6),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Nicholas Gregory"
        flavorText = "\"We are bound to wander this forest forever. Sometimes I think it wanders with us.\"\n—Ogni, Kannah scout"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/992aba3f-e1cb-4f5a-8b6f-328a069102a4.jpg?1783928206"
    }
}
