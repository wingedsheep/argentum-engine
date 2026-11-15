package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wardscale Dragon
 * {4}{W}{W}
 * Creature — Dragon
 * 4/4
 * Flying
 * As long as this creature is attacking, defending player can't cast spells.
 *
 * `Player.DefendingPlayer` as the prohibition's who-axis is the player this Dragon is attacking,
 * read live from combat — so "as long as this creature is attacking" needs no separate condition:
 * a Dragon that isn't attacking has no defending player, and in multiplayer only the player it
 * attacks (or the controller of the planeswalker it attacks) is locked.
 */
val WardscaleDragon = card("Wardscale Dragon") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\nAs long as this creature is attacking, defending player can't cast spells."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.DefendingPlayer)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4290215-3aec-40b8-aac5-e5c39c035cfe.jpg?1783938709"
    }
}
