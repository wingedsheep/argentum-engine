package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Creeping Corrosion — Mirrodin Besieged #78 (canonical / earliest real printing, 2011)
 * {2}{G}{G} · Sorcery
 *
 * Destroy all artifacts.
 */
val CreepingCorrosion = card("Creeping Corrosion") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy all artifacts."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Artifact)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Ryan Pancoast"
        flavorText = "\"We will reveal the futility of their heresy by showing them how fragile their " +
            "relics are.\"\n—Vorinclex, Voice of Hunger"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05d5a7b3-18b6-4b1d-85cc-2253e605390c.jpg?1783941377"
    }
}
