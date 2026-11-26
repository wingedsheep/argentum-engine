package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Flowering Lumberknot
 * {3}{G}
 * Creature — Treefolk
 * 5/5
 * This creature can't attack or block unless it's paired with a creature with soulbond.
 *
 * The restriction reads the live pairing through `pairedWith(...)` (StatePredicate.IsPairedWith),
 * so losing the partner (CR 702.95e) re-locks it at once.
 */
val FloweringLumberknot = card("Flowering Lumberknot") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    oracleText = "This creature can't attack or block unless it's paired with a creature with soulbond."
    power = 5
    toughness = 5

    val pairedWithSoulbond = Conditions.SourceMatches(
        GameObjectFilter.Creature.pairedWith(GameObjectFilter.Creature.withKeyword(Keyword.SOULBOND))
    )

    staticAbility {
        ability = CantAttackUnless(pairedWithSoulbond)
    }
    staticAbility {
        ability = CantBlockUnless(pairedWithSoulbond)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Nic Klein"
        flavorText = "\"It used to eat us trappers. Now it follows us around like some kind of hound dog.\"\n—Alena, trapper of Kessig"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78fa2ddc-142b-4562-8812-ecb72e3bae57.jpg?1783940669"
    }
}
