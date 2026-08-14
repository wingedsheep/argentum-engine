package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * City Pigeon
 * {W}
 * Creature — Bird
 * 1/1
 * Flying
 * When this creature leaves the battlefield, create a Food token. (It's an artifact
 * with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 */
val CityPigeon = card("City Pigeon") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    oracleText = "Flying\n" +
        "When this creature leaves the battlefield, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.CreateFood()
        description = "When this creature leaves the battlefield, create a Food token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "David Szabo"
        flavorText = "\"Aw, that was my lunch!\"\n—Spider-Man, Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56d67fb4-5b23-432c-9ffb-39545035c117.jpg?1783905365"
    }
}
