package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Storm the Festival
 * {3}{G}{G}{G}
 * Sorcery
 * Look at the top five cards of your library. You may put up to two permanent cards with mana
 * value 5 or less from among them onto the battlefield. Put the rest on the bottom of your library
 * in a random order.
 * Flashback {7}{G}{G}{G}
 */
val StormTheFestival = card("Storm the Festival") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may put up to two permanent cards with " +
        "mana value 5 or less from among them onto the battlefield. Put the rest on the bottom of your " +
        "library in a random order.\n" +
        "Flashback {7}{G}{G}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Permanent.manaValueAtMost(5),
            prompt = "Put up to two permanent cards with mana value 5 or less from among them onto the battlefield",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(2)),
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
        )
    }

    keywordAbility(KeywordAbility.flashback("{7}{G}{G}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Yigit Koroglu"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4f1f69c-122d-472a-b82d-fed7c253a4cd.jpg?1783925572"
    }
}
