package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crypt Angel
 * {4}{B}
 * Creature — Angel
 * 3/3
 * Flying, protection from white
 * When this creature enters, return target blue or red creature card from your graveyard to your hand.
 */
val CryptAngel = card("Crypt Angel") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying, protection from white\n" +
        "When this creature enters, return target blue or red creature card from your graveyard to your hand."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val card = target(
            "target blue or red creature card from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Creature
                        .ownedByYou()
                        .withAnyColor(Color.BLUE, Color.RED),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Todd Lockwood"
        flavorText = "Once an angel, now an abomination."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/522ddc6f-ec13-4a70-8f4c-b3c846b102fd.jpg?1562911661"
    }
}
