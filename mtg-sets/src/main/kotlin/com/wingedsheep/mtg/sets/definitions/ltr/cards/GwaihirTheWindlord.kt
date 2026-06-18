package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gwaihir the Windlord
 * {4}{W}{U}
 * Legendary Creature — Bird Noble
 * 4/4
 *
 * This spell costs {2} less to cast as long as you've drawn two or more cards this turn.
 * Flying, vigilance
 * Other Birds you control have vigilance.
 *
 * The conditional cost reduction uses `ModifySpellCost(SelfCast, ReduceGeneric(2),
 * gating = OnlyIf(Conditions.YouDrewCardsThisTurn(2)))` — the new "drew N+ cards this turn"
 * condition backed by the per-player `CardsDrawnThisTurnComponent`.
 */
val GwaihirTheWindlord = card("Gwaihir the Windlord") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Bird Noble"
    power = 4
    toughness = 4
    oracleText = "This spell costs {2} less to cast as long as you've drawn two or more cards this turn.\n" +
        "Flying, vigilance\n" +
        "Other Birds you control have vigilance."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.YouDrewCardsThisTurn(2)),
        )
    }

    // Other Birds you control have vigilance.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter(
                baseFilter = GameObjectFilter.Creature.youControl().withSubtype("Bird"),
                excludeSelf = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Axel Sauerwald"
        flavorText = "There came Gwaihir the Windlord, greatest of all the Eagles of the North, mightiest of the descendants of old Thorondor."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a42c951-1146-4f49-a690-7d385962b191.jpg?1686969841"
    }
}
