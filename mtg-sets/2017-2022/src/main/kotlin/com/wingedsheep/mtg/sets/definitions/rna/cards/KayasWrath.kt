package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kaya's Wrath
 * {W}{W}{B}{B}
 * Sorcery
 * Destroy all creatures. You gain life equal to the number of creatures you controlled that were
 * destroyed this way.
 *
 * The creatures are partitioned into yours / everyone else's *before* the wipe (controller
 * information is gone once they hit the graveyard), then the actually-destroyed set minus the
 * other players' half is counted — so regenerated or indestructible creatures of yours don't pay
 * off (mirrors Demonic Junker and Righteous Fury).
 */
val KayasWrath = card("Kaya's Wrath") {
    manaCost = "{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures. You gain life equal to the number of creatures you " +
        "controlled that were destroyed this way."

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(GameObjectFilter.Creature)
            val split = filterSplit(creatures, GameObjectFilter.Creature.youControl())
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Creature, storeDestroyedAs = it)
            }
            val destroyedYours = exclude(destroyed, split.rest)
            run(Effects.GainLife(destroyedYours.count))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Victor Adame Minguez"
        flavorText = "Teysa convened the meeting of the Obzedat. Kaya ended it."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ed140c1-752b-4539-88f2-1fa354049b17.jpg?1783933645"
    }
}
