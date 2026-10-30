package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blacklance Paragon
 * {1}{B}
 * Creature — Human Knight
 * 3/1
 *
 * Flash
 * When this creature enters, target Knight gains deathtouch and lifelink until end of turn.
 *
 * "Target Knight" is any Knight permanent — either player's, and including the Paragon itself.
 */
val BlacklanceParagon = card("Blacklance Paragon") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    oracleText = "Flash\n" +
        "When this creature enters, target Knight gains deathtouch and lifelink until end of turn."
    power = 3
    toughness = 1
    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val knight = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Knight")))
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, knight) then
            Effects.GrantKeyword(Keyword.LIFELINK, knight)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Victor Adame Minguez"
        flavorText = "The Blacklances travel deep into the wilds, sometimes for years at a time, in search of the lost Cauldron of Eternity."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe0a63bb-dd94-429f-aa9b-21f3d1c53ae5.jpg?1783932644"
    }
}
