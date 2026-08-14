package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Voyager Quickwelder — Aetherdrift #37
 * {2}{W} · Artifact Creature — Robot Artificer · 2/4
 *
 * Artifact spells you cast cost {1} less to cast.
 *
 * A plain generic-cost reduction on the caster's own artifact spells
 * ([SpellCostTarget.YouCast] + [CostModification.ReduceGeneric]). Generic-only, so it never
 * shaves a colored pip, and — like every cost static — it reduces the Quickwelder's *own*
 * successors, not itself: the ability only functions while it's on the battlefield.
 */
val VoyagerQuickwelder = card("Voyager Quickwelder") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot Artificer"
    power = 2
    toughness = 4
    oracleText = "Artifact spells you cast cost {1} less to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Artifact),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Kenn Yap"
        flavorText = "The Guidelight Voyagers had been stranded on Avishkar with no way to get home. " +
            "They considered every last unit essential, down to the glitchiest grunt."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6dcdc8c-fba1-4ea1-bf93-65072d10f0da.jpg?1783907912"
    }
}
