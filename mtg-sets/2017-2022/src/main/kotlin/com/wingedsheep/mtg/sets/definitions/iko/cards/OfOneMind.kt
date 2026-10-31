package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Of One Mind
 * {2}{U}
 * Sorcery
 * This spell costs {2} less to cast if you control a Human creature and a non-Human creature.
 * Draw two cards.
 */
val OfOneMind = card("Of One Mind") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if you control a Human creature and a non-Human creature.\n" +
        "Draw two cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.All(
                    Conditions.YouControl(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN)),
                    Conditions.YouControl(GameObjectFilter.Creature.notSubtype(Subtype.HUMAN))
                )
            )
        )
    }

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Matt Stewart"
        flavorText = "\"The *eludha* is total communication, total understanding. It makes regular human speech feel like garbled grunting.\"\n—Winota, heartbonder"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c95fb136-f21d-4f3a-82b7-bcf490b7e90c.jpg?1783931071"
    }
}
