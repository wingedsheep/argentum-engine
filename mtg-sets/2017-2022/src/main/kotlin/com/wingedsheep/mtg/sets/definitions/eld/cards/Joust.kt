package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Joust
 * {1}{R}
 * Sorcery
 * Choose target creature you control and target creature you don't control. The creature you
 * control gets +2/+1 until end of turn if it's a Knight. Then those creatures fight each other.
 *
 * The Knight check reads the creature as Joust resolves (projected state). If only the opposing
 * target is illegal, the Knight still gets +2/+1 but the fight does nothing (Outmuscle shape).
 */
val Joust = card("Joust") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose target creature you control and target creature you don't control. " +
        "The creature you control gets +2/+1 until end of turn if it's a Knight. " +
        "Then those creatures fight each other. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.KNIGHT), mine),
            then = Effects.ModifyStats(2, 1, mine),
        ) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Bram Sels"
        flavorText = "\"Life is a tournament that rewards the bold.\"\n—Syr Layne, knight of Embereth"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e725f04-5530-47f1-9f04-4cc13ed9348b.jpg?1783932622"
        ruling("2019-10-04", "The target creature you control gets +2/+1 only if it's a Knight as Joust resolves. It'll still fight if it doesn't get +2/+1.")
        ruling("2019-10-04", "If either target is an illegal target as Joust tries to resolve, neither creature will deal or be dealt damage.")
        ruling("2019-10-04", "If the creature you control is an illegal target as Joust tries to resolve, no creature gets +2/+1. If that creature is a legal target but the creature you don't control isn't, it still gets +2/+1 until end of turn if it's a Knight.")
    }
}
