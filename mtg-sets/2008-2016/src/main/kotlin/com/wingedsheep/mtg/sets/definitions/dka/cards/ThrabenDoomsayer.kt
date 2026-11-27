package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thraben Doomsayer
 * {1}{W}{W}
 * Creature — Human Cleric
 * 2/2
 * {T}: Create a 1/1 white Human creature token.
 * Fateful hour — As long as you have 5 or less life, other creatures you control get +2/+2.
 *
 * The fateful-hour lord is Gavony Ironwright's shape: [ModifyStats] over
 * [GroupFilter.OtherCreaturesYouControl] gated by [Conditions.LifeAtMost] 5, re-checked continuously.
 */
val ThrabenDoomsayer = card("Thraben Doomsayer") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{T}: Create a 1/1 white Human creature token.\n" +
        "Fateful hour — As long as you have 5 or less life, other creatures you control get +2/+2."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human")
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 2,
                filter = GroupFilter.OtherCreaturesYouControl
            ),
            condition = Conditions.LifeAtMost(5)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "John Stanko"
        flavorText = "\"Swarms of the unhallowed claw at Thraben's gates! Do you still deny the end approaches?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/066a9312-a5b2-4fc5-b46d-e0c9020583a5.jpg?1783940847"
        ruling(
            "2011-01-22",
            "A creature whose toughness is being increased by Thraben Doomsayer's fateful hour ability loses that " +
                "bonus as soon as its controller has 6 or more life (or as soon as Thraben Doomsayer leaves the " +
                "battlefield). If damage marked on that creature is greater than or equal to its toughness after " +
                "the fateful hour ability stops applying, the creature is destroyed and put into its owner's graveyard."
        )
        ruling(
            "2011-01-22",
            "If Thraben Doomsayer's activated ability resolves while you have 5 or less life, the creature token " +
                "will be 3/3 as it enters."
        )
    }
}
