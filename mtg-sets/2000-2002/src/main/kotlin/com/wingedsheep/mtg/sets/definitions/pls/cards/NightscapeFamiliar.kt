package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nightscape Familiar
 * {1}{B}
 * Creature — Zombie
 * 1/1
 *
 * Blue spells and red spells you cast cost {1} less to cast.
 * {1}{B}: Regenerate this creature.
 *
 * Modelling: one `ModifySpellCost` static whose filter is "blue or red"
 * (`withAnyColor(BLUE, RED)`), so a blue-and-red spell matches once and is discounted {1},
 * not {2} — exactly the 2022-12-08 ruling. `ReduceGeneric` never touches colored mana.
 */
val NightscapeFamiliar = card("Nightscape Familiar") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "Blue spells and red spells you cast cost {1} less to cast.\n" +
        "{1}{B}: Regenerate this creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.RED)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Jeff Easley"
        flavorText = "Nightscape masters don't stop at raising the spirit of a fallen battlemage. " +
            "They raise the flesh along with it."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24fa6853-09b0-4c9f-a138-9dd005780255.jpg?1783945620"

        ruling("2022-12-08", "A spell you cast that's blue and red costs {1} less, not {2} less.")
        ruling("2004-10-04", "Can never affect the colored part of the cost.")
        ruling("2004-10-04", "If this card is sacrificed to pay part of a spell's cost, the cost reduction still applies.")
    }
}
