package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Drider
 * {4}{B}
 * Creature — Elf Spider
 * 4/3
 * Reach
 * Whenever this creature deals combat damage to a player, create a 2/1 black Spider creature token with reach and menace.
 */
val Drider = card("Drider") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Spider"
    oracleText = "Reach\nWhenever this creature deals combat damage to a player, create a 2/1 black Spider creature token with reach and menace."
    power = 4
    toughness = 3
    keywords(Keyword.REACH)
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH, Keyword.MENACE)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Jodie Muir"
        flavorText = "Though their forms embody Lolth's image, driders are those unfortunate drow who have failed the Spider Queen."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbc06a88-1eb6-4edb-8f03-d8274560cc41.jpg?1783926498"
    }
}
