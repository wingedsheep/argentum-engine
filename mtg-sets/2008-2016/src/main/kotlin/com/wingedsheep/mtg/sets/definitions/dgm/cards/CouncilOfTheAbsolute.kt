package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Council of the Absolute — Dragon's Maze #62.
 *
 * The as-enters name is an [EntersWithChoice] `CARD_NAME` choice from the
 * [CardNamePool.NONCREATURE_NONLAND] pool. The opponents' cast ban ([PlayersCantCastSpells],
 * Alhammarret's shape) and the controller's {2} discount ([ModifySpellCost] over
 * [SpellCostTarget.YouCast]) both read the name through `namedFromChosenComponent()`.
 */
val CouncilOfTheAbsolute = card("Council of the Absolute") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Advisor"
    power = 2
    toughness = 4
    oracleText = "As this creature enters, choose a noncreature, nonland card name.\n" +
        "Your opponents can't cast spells with the chosen name.\n" +
        "Spells with the chosen name you cast cost {2} less to cast."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONCREATURE_NONLAND,
        )
    )

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.namedFromChosenComponent(),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.namedFromChosenComponent()),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "62"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da18a6a5-0042-40ae-bd33-a6d5a65a9944.jpg?1783940031"
    }
}
