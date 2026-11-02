package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Alpine Moon
 * {R}
 * Enchantment
 *
 * As this enchantment enters, choose a nonbasic land card name.
 * Lands your opponents control with the chosen name lose all land types and abilities, and they
 * gain "{T}: Add one mana of any color."
 *
 * The name is stored on the permanent ([EntersWithChoice] with [CardNamePool.NONBASIC_LAND]) and read
 * by both statics through `namedFromChosenComponent()`. [SetLandTypesForGroup] with no land types is
 * "lose all land types and abilities" (layer 4 + layer 6); the granted mana ability is a later
 * layer-6 grant from the same source, so it survives the removal.
 */
val AlpineMoon = card("Alpine Moon") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a nonbasic land card name.\n" +
        "Lands your opponents control with the chosen name lose all land types and abilities, " +
        "and they gain \"{T}: Add one mana of any color.\""

    replacementEffect(EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.NONBASIC_LAND))

    val affected = GroupFilter(GameObjectFilter.Land.opponentControls().namedFromChosenComponent())

    staticAbility {
        ability = SetLandTypesForGroup(filter = affected, landTypes = emptySet())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            filter = affected,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Alayna Danner"
        flavorText = "\"A rare and fascinating phenomenon.\"\n—Tamiyo"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2435c810-2baf-4e3b-80ce-542b94694901.jpg?1783934557"
        ruling("2018-07-13", "Alpine Moon's effect doesn't affect supertypes or other card types. It won't remove the artifact card type from an artifact land or remove the legendary supertype from a legendary land.")
        ruling("2018-07-13", "If an affected land has an ability that triggers \"when\" it enters the battlefield, it will lose that ability before it triggers.")
        ruling("2018-07-13", "If an affected land has an ability that causes it to enter the battlefield tapped, it will lose that ability before it applies. The same is also true of any other abilities that modify how a land enters the battlefield or apply \"as\" a land enters the battlefield.")
    }
}
