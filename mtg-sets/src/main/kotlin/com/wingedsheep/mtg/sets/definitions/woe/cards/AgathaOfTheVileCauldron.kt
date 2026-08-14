package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** Agatha of the Vile Cauldron — Wilds of Eldraine #199. */
val AgathaOfTheVileCauldron = card("Agatha of the Vile Cauldron") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warlock"
    oracleText = "Activated abilities of creatures you control cost {X} less to activate, where X is " +
        "Agatha's power. This effect can't reduce the mana in that cost to less than one mana.\n" +
        "{4}{R}{G}: Other creatures you control get +1/+1 and gain trample and haste until end of turn."
    power = 1
    toughness = 1

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            amount = DynamicAmounts.sourcePower(),
            manaFloor = 1
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{R}{G}")
        effect = Effects.Composite(
            Patterns.Group.modifyStatsForAll(
                power = 1,
                toughness = 1,
                filter = GroupFilter(GameObjectFilter.Creature.youControl()).other(),
                duration = Duration.EndOfTurn
            ),
            Patterns.Group.grantKeywordToAll(
                Keyword.TRAMPLE,
                GroupFilter(GameObjectFilter.Creature.youControl()).other(),
                Duration.EndOfTurn
            ),
            Patterns.Group.grantKeywordToAll(
                Keyword.HASTE,
                GroupFilter(GameObjectFilter.Creature.youControl()).other(),
                Duration.EndOfTurn
            )
        )
        description = "Other creatures you control get +1/+1 and gain trample and haste until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6c48f07-63b7-4a60-8da6-ce77405abf1e.jpg?1783915073"
        ruling(
            "2023-09-01",
            "Agatha of the Vile Cauldron's first ability affects only abilities of creatures you control " +
                "on the battlefield. The costs of activated abilities of creature cards that work in " +
                "other zones (such as cycling) won't be reduced."
        )
    }
}
