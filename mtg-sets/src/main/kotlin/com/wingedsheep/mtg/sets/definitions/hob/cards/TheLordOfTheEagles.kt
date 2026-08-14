package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * The Lord of the Eagles
 * {7}{U}{U}
 * Legendary Creature — Bird Noble
 * 8/8
 *
 * Flash
 * This spell costs {X} less to cast, where X is the total power of creatures you control with flying.
 * Flying
 *
 * A filtered Ghalta: the same [SpellCostTarget.SelfCast] +
 * [CostModification.ReduceGenericBy] rail, sourced from
 * [CostReductionSource.TotalPropertyAmongPermanentsYouControl] over flying creatures. The filter is
 * evaluated against projected state, so a creature that only has flying from a granted keyword (or
 * an animated flying artifact) counts, and Ghalta's rulings carry over: the total is locked in
 * before payment, the reduction never eats the coloured `{U}{U}`, and this creature's own 8 power
 * doesn't count because it is still on the stack while its cost is calculated.
 */
val TheLordOfTheEagles = card("The Lord of the Eagles") {
    manaCost = "{7}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Bird Noble"
    power = 8
    toughness = 8
    oracleText = "Flash\n" +
        "This spell costs {X} less to cast, where X is the total power of creatures you control " +
        "with flying.\n" +
        "Flying"

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.TotalPropertyAmongPermanentsYouControl(
                    property = EntityNumericProperty.Power,
                    filter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Zezhou Chen"
        flavorText = "The ancient Eagles of the northern mountains were proud, strong, and " +
            "noble-hearted—the greatest of all birds."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa0554fc-9448-4ae2-8712-4f4f7af3c7b4.jpg?1784636060"
    }
}
