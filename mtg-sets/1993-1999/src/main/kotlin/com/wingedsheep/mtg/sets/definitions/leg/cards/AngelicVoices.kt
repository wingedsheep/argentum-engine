package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Angelic Voices
 * {2}{W}{W}
 * Enchantment
 *
 * Creatures you control get +1/+1 as long as you control no nonartifact, nonwhite creatures.
 *
 * The switch is a live "you control no …" check, so the anthem turns on and off as creatures
 * enter, leave, or change colour. A creature only breaks it if it is *neither* an artifact *nor*
 * white: a white-green creature or a red artifact creature keeps the bonus on (2004-10-04 ruling).
 */
val AngelicVoices = card("Angelic Voices") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1 as long as you control no nonartifact, nonwhite creatures."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 1,
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            ),
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.nonartifact().notColor(Color.WHITE),
                negate = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8068c263-e5fa-4449-8887-418e9d0a4da4.jpg?1783948088"
        ruling(
            "2004-10-04",
            "You get the bonus if all of your creatures are white or artifact even if they also have other " +
                "colors or qualities. So, if your only creature is a white and green creature, or a red " +
                "artifact creature, you would get the bonus."
        )
    }
}
