package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayForetellOnAnyTurn
import com.wingedsheep.sdk.scripting.ModifySpecialActionCost
import com.wingedsheep.sdk.scripting.SpecialActionKind

/**
 * Cosmos Charger (Kaldheim #51)
 * {3}{U} · Creature — Horse Spirit 3/3
 * Flash
 * Flying
 * Foretelling cards from your hand costs {1} less and can be done on any player's turn.
 * Foretell {2}{U}
 *
 * The third ability is two statics: [ModifySpecialActionCost] on the foretell special action's {2},
 * and [MayForetellOnAnyTurn]. Both work only while Cosmos Charger is on the battlefield, so neither
 * touches foretelling Cosmos Charger itself from hand.
 */
val CosmosCharger = card("Cosmos Charger") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Horse Spirit"
    power = 3
    toughness = 3
    oracleText = "Flash\nFlying\nForetelling cards from your hand costs {1} less and can be done on any player's turn.\n" +
        "Foretell {2}{U} (During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost.)"

    keywords(Keyword.FLASH, Keyword.FLYING)
    keywordAbility(KeywordAbility.foretell("{2}{U}"))

    staticAbility {
        ability = ModifySpecialActionCost(SpecialActionKind.FORETELL, CostModification.ReduceGeneric(1))
    }
    staticAbility {
        ability = MayForetellOnAnyTurn
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ec318c6-b718-436f-b9e8-e0c6154e5010.jpg?1783928268"
    }
}
