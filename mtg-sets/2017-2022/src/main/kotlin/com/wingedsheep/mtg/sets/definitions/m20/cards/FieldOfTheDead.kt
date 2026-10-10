package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Field of the Dead — Core Set 2020 #247 (canonical printing)
 * (no mana cost) · Land
 *
 * This land enters tapped.
 * {T}: Add {C}.
 * Whenever this land or another land you control enters, if you control seven or more lands with
 * different names, create a 2/2 black Zombie creature token.
 *
 * "This land or another land you control" is exactly "a land you control" (Field is itself a land
 * you control), so the trigger is [Triggers.a] over lands you control. The seven-names clause is an
 * intervening "if" (CR 603.4): checked when the trigger fires and again on resolution, counting
 * Field itself and every land that entered simultaneously (the Scapeshift ruling).
 */
val FieldOfTheDead = card("Field of the Dead") {
    manaCost = ""
    typeLine = "Land"
    oracleText =
        "This land enters tapped.\n" +
        "{T}: Add {C}.\n" +
        "Whenever this land or another land you control enters, if you control seven or more lands " +
        "with different names, create a 2/2 black Zombie creature token."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.differentlyNamedLandsYouControl(),
            ComparisonOperator.GTE,
            7
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/1/8/18f0436e-9328-4266-9cf8-80b557a0c17c.jpg?1783932856"
        )
        description = "Whenever this land or another land you control enters, if you control seven " +
            "or more lands with different names, create a 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/470ca3f4-29aa-4c4c-8ff2-8cdd70c69943.jpg?1783932937"
        ruling(
            "2019-07-12",
            "Field of the Dead's last ability counts itself and the land entering the battlefield in " +
                "addition to whichever other lands you control."
        )
        ruling(
            "2019-07-12",
            "If you control multiple lands with the same name, only one of those lands will count toward " +
                "the seven or more required to create a Zombie. For example, if you control four lands " +
                "named Plains, two named Island, and one named Field of the Dead, you control three lands " +
                "with different names."
        )
        ruling(
            "2019-07-12",
            "If multiple lands enter the battlefield simultaneously, possibly including Field of the Dead " +
                "itself, all of those lands are counted. For example, if you sacrifice eight lands while " +
                "resolving Scapeshift and search your library for five different basic land cards, two " +
                "Field of the Dead cards, and one other land card with a different name, you'll create " +
                "sixteen Zombie tokens."
        )
    }
}
