package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Sylvan Awakening
 * {2}{G}
 * Sorcery
 * Until your next turn, all lands you control become 2/2 Elemental creatures with reach,
 * indestructible, and haste. They're still lands.
 */
val SylvanAwakening = card("Sylvan Awakening") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until your next turn, all lands you control become 2/2 Elemental creatures with reach, indestructible, and haste. They're still lands."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Land.youControl()),
            effect = BecomeCreatureEffect(
                target = EffectTarget.Self,
                power = 2,
                toughness = 2,
                keywords = setOf(Keyword.REACH, Keyword.INDESTRUCTIBLE, Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                duration = Duration.UntilYourNextTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Adam Paquette"
        flavorText = "\"Yavimaya is aware, but not always awake. You will know the difference when you see it.\"\n—Karn"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93ac4d3d-064e-459d-b3a4-6c0872a2da8c.jpg?1591104718"
        ruling("2018-04-27", "Sylvan Awakening doesn't untap any of the lands that become creatures.")
        ruling("2018-04-27", "Sylvan Awakening affects only lands you control at the time it resolves. Lands you begin to control before your next turn won't become creatures.")
    }
}
