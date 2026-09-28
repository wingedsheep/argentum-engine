package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Karsus Depthguard — March of the Machine #150.
 * {2}{R} · Creature — Lizard Warrior 4/3
 *
 * "Can attack as though it didn't have defender" is the self-scoped [CanAttackDespiteDefender],
 * gated on the source's current (projected) power.
 */
val KarsusDepthguard = card("Karsus Depthguard") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    power = 4
    toughness = 3
    oracleText = "Defender\nAs long as this creature's power is 5 or greater, it can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.sourcePower(),
                ComparisonOperator.GTE,
                5,
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Tyler Jacobson"
        flavorText = "He stood his ground in the Mirrored Depths, his roar of defiance echoed by a thousand crystalline reflections."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ac76be5-3a2c-499e-830a-bd02eec517ce.jpg?1783916987"
        ruling("2023-04-14", "Once Karsus Depthguard has legally attacked, causing its last ability to not apply by reducing its power to 4 or less won't cause it to stop attacking.")
    }
}
