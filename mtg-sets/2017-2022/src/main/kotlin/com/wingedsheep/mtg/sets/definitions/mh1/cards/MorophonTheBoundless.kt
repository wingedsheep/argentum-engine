package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Morophon, the Boundless
 * {7}
 * Legendary Creature — Shapeshifter
 * 6/6
 *
 * Changeling (This card is every creature type.)
 * As Morophon enters, choose a creature type.
 * Spells of the chosen type you cast cost {W}{U}{B}{R}{G} less to cast. This effect reduces only
 * the amount of colored mana you pay.
 * Other creatures you control of the chosen type get +1/+1.
 *
 * [CostModification.ReduceColored] never spills into generic mana, and it reduces a hybrid or
 * Phyrexian pip of a reduced color too ({2}{W/U}{W/U} becomes {2}, per the ruling below).
 */
val MorophonTheBoundless = card("Morophon, the Boundless") {
    manaCost = "{7}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Shapeshifter"
    power = 6
    toughness = 6
    oracleText = "Changeling (This card is every creature type.)\n" +
        "As Morophon enters, choose a creature type.\n" +
        "Spells of the chosen type you cast cost {W}{U}{B}{R}{G} less to cast. This effect reduces only the amount of colored mana you pay.\n" +
        "Other creatures you control of the chosen type get +1/+1."

    keywords(Keyword.CHANGELING)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withChosenSubtype()),
            modification = CostModification.ReduceColored("{W}{U}{B}{R}{G}"),
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures(excludeSelf = true).youControl()
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9693e59b-032d-4ddc-a7d1-88a0f52dcc6c.jpg?1783933166"

        ruling("2019-06-14", "You must choose an existing creature type, such as Sliver or Warrior. Card types such as artifact, and supertypes such as legendary or snow, can't be chosen.")
        ruling("2019-06-14", "Morophon's effect reduces the total cost by up to one mana of each color. For example, if a spell of the chosen type costs {4}{R}{W}{W}, it will cost {4}{W} after applying Morophon's effect.")
        ruling("2019-06-14", "If a spell has hybrid mana symbols in its mana cost, you choose which half you will be paying before determining the total cost. For example, if a spell of the chosen type costs {2}{W/U}{W/U}, you may choose for the cost to be {2}{W}{U} and then reduce it to {2}.")
        ruling("2019-06-14", "Cost reduction effects are applied after other cost modifiers, so Morophon can reduce additional costs or alternative costs of spells of the chosen type.")
        ruling("2019-06-14", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to other creatures you control of the chosen type may become lethal if Morophon leaves the battlefield during that turn.")
    }
}
