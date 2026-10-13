package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

val VineshaperProdigy = card("Vineshaper Prodigy") {
    manaCost = "{1}{G}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 2
    oracleText = "Kicker {1}{U} (You may pay an additional {1}{U} as you cast this spell.)\nWhen this creature enters, if it was kicked, look at the top three cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order."

    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Inka Schulz"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/545cddde-8018-4fdd-874d-b5193b73b414.jpg?1783921290"
    }
}
