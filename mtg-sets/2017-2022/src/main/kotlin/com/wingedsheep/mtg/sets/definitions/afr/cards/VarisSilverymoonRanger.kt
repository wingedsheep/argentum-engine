package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Varis, Silverymoon Ranger
 * {1}{G}{G}
 * Legendary Creature — Human Elf Ranger
 * 3/3
 * Reach, ward {1}
 * Whenever you cast a creature or planeswalker spell, venture into the dungeon. This ability
 * triggers only once each turn.
 * Whenever you complete a dungeon, create a 2/2 green Wolf creature token.
 */
val VarisSilverymoonRanger = card("Varis, Silverymoon Ranger") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Elf Ranger"
    power = 3
    toughness = 3
    oracleText = "Reach, ward {1}\n" +
        "Whenever you cast a creature or planeswalker spell, venture into the dungeon. This ability triggers only once each turn. (To venture into the dungeon, enter the first room or advance to the next room.)\n" +
        "Whenever you complete a dungeon, create a 2/2 green Wolf creature token."

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.CreatureOrPlaneswalker)
        oncePerTurn = true
        effect = Effects.VentureIntoTheDungeon()
    }

    triggeredAbility {
        trigger = Triggers.you.completesADungeon()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf")
        )
        description = "Whenever you complete a dungeon, create a 2/2 green Wolf creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3bd3ea9-f06d-4df7-8b8d-ac6a0e591b09.jpg?1783926453"
    }
}
