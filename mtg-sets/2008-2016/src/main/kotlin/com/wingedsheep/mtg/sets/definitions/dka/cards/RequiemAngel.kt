package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Requiem Angel
 * {5}{W}
 * Creature — Angel
 * 5/5
 * Flying
 * Whenever another non-Spirit creature you control dies, create a 1/1 white Spirit creature token
 * with flying.
 */
val RequiemAngel = card("Requiem Angel") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever another non-Spirit creature you control dies, create a 1/1 white Spirit creature token with flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.notSubtype(Subtype.SPIRIT).youControl()).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Eric Deschamps"
        flavorText = "When angels despair, what hope can remain for mortals?"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5385925d-05ad-4f2e-bd2c-8de6c088ed05.jpg"
    }
}
