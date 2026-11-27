package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elgaud Inquisitor
 * {3}{W}
 * Creature — Human Cleric
 * 2/2
 * Lifelink
 * When this creature dies, create a 1/1 white Spirit creature token with flying.
 */
val ElgaudInquisitor = card("Elgaud Inquisitor") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "When this creature dies, create a 1/1 white Spirit creature token with flying."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Slawomir Maniak"
        flavorText = "\"It will take more than steel alone to purge this world of evil.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c342e1da-7ab9-4e29-96e6-77d820a45ede.jpg"
    }
}
