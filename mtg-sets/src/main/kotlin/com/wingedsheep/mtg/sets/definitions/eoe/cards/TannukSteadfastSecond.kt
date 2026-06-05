package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWarpToCardsInHand
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tannuk, Steadfast Second
 * {2}{R}{R}
 * Legendary Creature — Kavu Pilot
 * 3/5
 * Other creatures you control have haste.
 * Artifact cards and red creature cards in your hand have warp {2}{R}.
 */
val TannukSteadfastSecond = card("Tannuk, Steadfast Second") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Kavu Pilot"
    power = 3
    toughness = 5
    oracleText = "Other creatures you control have haste.\n" +
        "Artifact cards and red creature cards in your hand have warp {2}{R}. " +
        "(You may cast a card from your hand for its warp cost. Exile that permanent at the " +
        "beginning of the next end step, then you may cast it from exile on a later turn.)"

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other(),
        )
    }

    staticAbility {
        ability = GrantWarpToCardsInHand(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature.withColor(Color.RED),
            cost = ManaCost.parse("{2}{R}"),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Raymond Swanland"
        flavorText = "\"We're a few bones short of a skeleton crew, Sami, but we've got plenty of spine.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44607ed3-9523-40ac-9f61-0edd011cf762.jpg?1752947211"
    }
}
