package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Academy Wall
 * {2}{U}
 * Creature — Wall
 * 0/5
 * Defender
 * Whenever you cast an instant or sorcery spell, you may draw a card. If you do, discard a card.
 * This ability triggers only once each turn.
 *
 * "May draw; if you do, discard" is [Effects.May] over `Patterns.Hand.loot()`, so declining
 * draws and discards nothing. "Triggers only once each turn" is the `oncePerTurn` firing cap.
 */
val AcademyWall = card("Academy Wall") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 5
    oracleText = "Defender\n" +
        "Whenever you cast an instant or sorcery spell, you may draw a card. If you do, discard a card. " +
        "This ability triggers only once each turn."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        oncePerTurn = true
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Adam Paquette"
        flavorText = "Once the Society of Mishra took control, the Tolarian Academy at Lat-Nam became a haven for dark research."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2028115-f0de-4e8b-99bc-7369352e1e07.jpg?1783921352"
    }
}
