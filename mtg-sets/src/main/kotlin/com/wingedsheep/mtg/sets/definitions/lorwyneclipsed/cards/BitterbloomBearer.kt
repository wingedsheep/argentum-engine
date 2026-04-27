package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val BitterbloomBearer = card("Bitterbloom Bearer") {
    manaCost = "{B}{B}"
    typeLine = "Creature — Faerie Rogue"
    power = 1
    toughness = 1
    oracleText = "Flash\nFlying\n" +
        "At the beginning of your upkeep, you lose 1 life and create a 1/1 blue and black Faerie creature token with flying."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = CompositeEffect(listOf(
            Effects.LoseLife(1, EffectTarget.Controller),
            CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLUE, Color.BLACK),
                creatureTypes = setOf("Faerie"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/0/1/01524db2-c96f-4902-8394-bc7a7128e573.jpg?1767956498"
            )
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Chris Rahn"
        flavorText = "Petals burst in evening hours with flitting things so fickle and sour."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7127164d-f2a3-4d79-b6db-93507ff5ab47.jpg?1759144841"
    }
}
