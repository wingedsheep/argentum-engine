package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Esquire of the King
 * {W}
 * Creature — Human Soldier
 * 1/1
 *
 * {4}{W}, {T}: Creatures you control get +1/+1 until end of turn. This ability
 * costs {2} less to activate if you control a legendary creature.
 */
val EsquireOfTheKing = card("Esquire of the King") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "{4}{W}, {T}: Creatures you control get +1/+1 until end of turn. This ability costs {2} less to activate if you control a legendary creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}"), Costs.Tap)
        effect = GroupPatterns.modifyStatsForAll(
            1, 1,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
        // "costs {2} less to activate if you control a legendary creature"
        genericCostReduction = DynamicAmount.Conditional(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.legendary()),
            ifTrue = DynamicAmount.Fixed(2),
            ifFalse = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Tyukina Tatiana"
        flavorText = "\"Where now the horse and the rider?\nWhere is the horn that was blowing?\nWhere is the helm and the hauberk,\nand the bright hair flowing?\"\n—Lament for the Rohirrim"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caa6dded-ab08-43d0-b2fb-008854582cba.jpg?1686967754"
    }
}
