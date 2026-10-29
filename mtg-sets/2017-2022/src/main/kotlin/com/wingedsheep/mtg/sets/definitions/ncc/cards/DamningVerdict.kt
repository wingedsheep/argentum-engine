package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Damning Verdict — New Capenna Commander #15
 * {3}{W}{W} · Sorcery
 *
 * Destroy all creatures with no counters on them.
 *
 * "No counters" is counters of any kind (Heartless Act's `withoutCounters()`), read as the spell
 * resolves.
 */
val DamningVerdict = card("Damning Verdict") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures with no counters on them."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.withoutCounters())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Gaboleps"
        flavorText = "\"Guilty as charged!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5be40c34-6df0-4471-b99b-850ae2be9923.jpg?1783923375"
    }
}
