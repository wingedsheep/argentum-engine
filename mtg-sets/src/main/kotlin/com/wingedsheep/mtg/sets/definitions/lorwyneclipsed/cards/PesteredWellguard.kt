package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

val PesteredWellguard = card("Pestered Wellguard") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Merfolk Soldier"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature becomes tapped, create a 1/1 blue and black Faerie creature token with flying."

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = CreateTokenEffect(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Faerie"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/0/1/01524db2-c96f-4902-8394-bc7a7128e573.jpg?1767956498",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Julie Dillon"
        flavorText = "In silence, she cursed a thousand times the fact she'd been stationed near fae lands."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e06c99e-ecb2-42e9-ac58-2542de8d54a5.jpg?1767871787"
    }
}
