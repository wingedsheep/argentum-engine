package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Anointed Peacekeeper — Dominaria United #2.
 *
 * The tax reaches every activated ability of a source with the chosen name in any zone: a hand
 * card's cycling, and a Vehicle's crew ("{2}, tap creatures with total power N").
 */
val AnointedPeacekeeper = card("Anointed Peacekeeper") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "As this creature enters, look at an opponent's hand, then choose any card name.\n" +
        "Spells your opponents cast with the chosen name cost {2} more to cast.\n" +
        "Activated abilities of sources with the chosen name cost {2} more to activate unless they're mana abilities."

    keywords(Keyword.VIGILANCE)

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
            lookAtOpponentHand = true,
        )
    )

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any.namedFromChosenComponent()),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    staticAbility {
        ability = IncreaseActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Any.namedFromChosenComponent()),
            amount = DynamicAmounts.fixed(2),
            excludeManaAbilities = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Tia Masic"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b8127b5-3a65-411a-84bc-54e5c1be1477.jpg?1783921372"
    }
}
