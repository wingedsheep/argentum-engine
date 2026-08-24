package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ravages of War
 * {3}{W}
 * Sorcery
 * Destroy all lands.
 *
 * The white Armageddon. [Effects.DestroyAll] is the wrath *pipeline* — gather the matching
 * battlefield cards first, then move the whole collection at once — so every land leaves
 * simultaneously and the leaves-the-battlefield triggers all see the same board.
 */
val RavagesOfWar = card("Ravages of War") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all lands."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Fang Yue"
        flavorText = "\"Thorn bushes spring up wherever the army has passed. Lean years follow in the wake of a great war.\"\n—Lao Tzu, *Tao Te Ching* (trans. Feng and English)"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11dca9ba-b27f-4af8-9962-3794e743886f.jpg"
    }
}
