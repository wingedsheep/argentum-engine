package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paragon of Open Graves
 * {3}{B}
 * Creature — Skeleton Warrior
 * 2/2
 * Other black creatures you control get +1/+1.
 * {2}{B}, {T}: Another target black creature you control gains deathtouch until end of turn. (Any amount of damage it deals to a creature is enough to destroy it.)
 */
val ParagonOfOpenGraves = card("Paragon of Open Graves") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Warrior"
    power = 2
    toughness = 2
    oracleText = "Other black creatures you control get +1/+1.\n" +
        "{2}{B}, {T}: Another target black creature you control gains deathtouch until end of turn. (Any amount of damage it deals to a creature is enough to destroy it.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.BLACK).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.withColor(Color.BLACK).youControl().other())
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33d7de75-b6e1-44f5-ae1c-e095b41e8d0b.jpg"
    }
}
