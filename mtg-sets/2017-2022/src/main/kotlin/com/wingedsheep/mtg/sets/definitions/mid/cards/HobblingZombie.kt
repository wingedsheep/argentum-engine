package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hobbling Zombie
 * {2}{B}
 * Creature — Zombie
 * 2/2
 * Deathtouch
 * When this creature dies, create a 2/2 black Zombie creature token with decayed.
 */
val HobblingZombie = card("Hobbling Zombie") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\nWhen this creature dies, create a 2/2 black Zombie creature token with decayed. " +
        "(It can't block. When it attacks, sacrifice it at end of combat.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Josh Hass"
        flavorText = "\"I see someone tried to stop you, my pet. How delightful.\"\n—Ghoulcaller Gisa"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68284193-a38e-40d2-8d15-92d223756751.jpg?1783925616"
    }
}
