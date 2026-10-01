package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Distorted Curiosity — Phyrexia: All Will Be One #46
 * {2}{U}
 * Sorcery
 * Corrupted — This spell costs {2} less to cast if an opponent has three or more poison counters.
 * Draw two cards.
 *
 * The corrupted discount is a self-cast [ModifySpellCost] gated on [Conditions.Corrupted], the
 * same rail Squash uses, so the reduced cost shows in the client's cost preview. It only eats
 * generic mana, so the floor is {U}; the mana value stays 3.
 */
val DistortedCuriosity = card("Distorted Curiosity") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Corrupted — This spell costs {2} less to cast if an opponent has three or more poison counters.\n" +
        "Draw two cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.Corrupted),
        )
    }

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Svetlin Velinov"
        flavorText = "\"Free will was an illusion so perfect it fooled even me. I know better now.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f932ddd3-beb4-4dc8-8c15-e1c9c2276986.jpg?1783918068"
    }
}
