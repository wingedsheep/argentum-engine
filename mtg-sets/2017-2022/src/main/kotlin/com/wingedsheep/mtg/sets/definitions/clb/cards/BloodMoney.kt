package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Blood Money — Commander Legends: Battle for Baldur's Gate #116 (canonical printing)
 * {5}{B}{B} · Sorcery
 *
 * Destroy all creatures. For each nontoken creature destroyed this way, you create a tapped
 * Treasure token.
 *
 * Counts only creatures that were actually destroyed (an indestructible or regenerated one is
 * neither destroyed nor paid for) *and* were nontoken. The nontoken creatures are gathered before
 * the sweep — a destroyed token has ceased to exist and can't be tested afterwards — and the
 * destroyed set is intersected with them via two set differences (A ∩ D = A − (A − D)). The sweep
 * itself is one simultaneous [Effects.DestroyAll], so creatures see each other die together.
 */
val BloodMoney = card("Blood Money") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures. For each nontoken creature destroyed this way, you create a tapped Treasure token."

    spell {
        effect = Effects.Pipeline {
            val nontoken = gather(GameObjectFilter.Creature.nontoken())
            val destroyed = runStoringCollection { Effects.DestroyAll(GameObjectFilter.Creature, storeDestroyedAs = it) }
            val nontokenSurvivors = exclude(nontoken, destroyed)
            val nontokenDestroyed = exclude(nontoken, nontokenSurvivors)
            run(Effects.CreateTreasure(count = nontokenDestroyed.count, tapped = true))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "116"
        artist = "Inka Schulz"
        flavorText = "For the patriar class, wealth is in the blood."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d45c18c-b8eb-465c-8dfc-fd6da73e25b5.jpg?1783922766"
    }
}
