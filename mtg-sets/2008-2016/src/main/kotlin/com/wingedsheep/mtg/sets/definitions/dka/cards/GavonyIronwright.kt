package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gavony Ironwright
 * {2}{W}
 * Creature — Human Soldier
 * 1/4
 * Fateful hour — As long as you have 5 or less life, other creatures you control get +1/+4.
 *
 * Weapons Trainer's conditional lord shape: [ModifyStats] over
 * [GroupFilter.OtherCreaturesYouControl] gated by [Conditions.LifeAtMost] 5. The condition is
 * re-checked continuously, so gaining life above 5 switches the bonus off at once.
 */
val GavonyIronwright = card("Gavony Ironwright") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 4
    oracleText = "Fateful hour — As long as you have 5 or less life, other creatures you control get +1/+4."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 4,
                filter = GroupFilter.OtherCreaturesYouControl
            ),
            condition = Conditions.LifeAtMost(5)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Karl Kopinski"
        flavorText = "\"I wish my work could shelter everyone, yet I pray we are never so few that it does.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05d8de75-b169-4426-94a4-b19cdfdffd89.jpg?1783940854"
    }
}
