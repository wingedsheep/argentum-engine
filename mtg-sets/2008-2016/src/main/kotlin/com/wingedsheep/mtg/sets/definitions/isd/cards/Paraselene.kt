package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Paraselene — Innistrad #26
 * {2}{W} · Sorcery
 *
 * Destroy all enchantments. You gain 1 life for each enchantment destroyed this way.
 *
 * Fumigate's shape: [Effects.DestroyAll] with `storeDestroyedAs` records only the enchantments
 * actually destroyed (indestructible / regenerated ones stay out of the count), and that
 * collection's count is the life gained.
 */
val Paraselene = card("Paraselene") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all enchantments. You gain 1 life for each enchantment destroyed this way."

    spell {
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Enchantment, storeDestroyedAs = it)
            }
            run(Effects.GainLife(destroyed.count))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Ryan Yee"
        flavorText = "\"Moonlight has a way of showing all things as they truly are—for better or for worse.\"\n—Zilla of Lambholt"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/406380ab-2695-4084-99a5-f5560304f8cb.jpg?1783940989"
    }
}
