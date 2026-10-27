package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.PlayersCantPayLifeForCosts
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Angel of Jubilation — Avacyn Restored #2
 * {1}{W}{W}{W} · Creature — Angel · 3/3
 *
 * Flying
 * Other nonblack creatures you control get +1/+1.
 * Players can't pay life or sacrifice creatures to cast spells or activate abilities.
 *
 * The last ability is two prohibitions on paying a cost, each scoped to casting spells and
 * activating abilities (mana abilities included): [PlayersCantPayLifeForCosts] forbids the life,
 * and [CantBeSacrificed] with [CantBeSacrificed.Scope.CASTING_AND_ACTIVATION_COSTS] over every
 * creature forbids the sacrifice. Per the rulings it is the action that's stopped, not the wording:
 * an artifact creature can't pay "Sacrifice an artifact" while a noncreature artifact still can, and
 * a resolving spell or ability may still make a player pay life or sacrifice a creature.
 */
val AngelOfJubilation = card("Angel of Jubilation") {
    manaCost = "{1}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Other nonblack creatures you control get +1/+1.\n" +
        "Players can't pay life or sacrifice creatures to cast spells or activate abilities."
    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.notColor(Color.BLACK).youControl(), excludeSelf = true),
        )
    }

    staticAbility {
        ability = PlayersCantPayLifeForCosts()
    }

    staticAbility {
        ability = CantBeSacrificed(GroupFilter.AllCreatures, CantBeSacrificed.Scope.CASTING_AND_ACTIVATION_COSTS)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Terese Nielsen"
        flavorText = "\"Though Innistrad is not cleansed of evil, we finally have cause to rejoice.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16c5dfed-4dee-4e48-a445-89f03d7794e6.jpg?1783940743"
    }
}
