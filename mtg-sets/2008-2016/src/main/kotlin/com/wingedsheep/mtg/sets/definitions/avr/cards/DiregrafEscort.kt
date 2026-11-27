package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Diregraf Escort — Avacyn Restored #174 (canonical printing)
 * {G} · Creature — Human Cleric · 1/1
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have protection from
 * Zombies.
 *
 * [Wingcrafter]'s shape with [GrantProtectionFromToGroup] in place of a keyword grant:
 * `GroupFilter.soulbondPair()` covers "both creatures" and is empty while unpaired.
 * `ProtectionScope.Subtype("Zombie")` carries no card type, so any Zombie source counts.
 */
val DiregrafEscort = card("Diregraf Escort") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Cleric"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have protection " +
        "from Zombies."
    power = 1
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantProtectionFromToGroup(
            scope = ProtectionScope.Subtype("Zombie"),
            filter = GroupFilter.soulbondPair()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/640e21ad-5064-41bc-886e-2c997f69a3f5.jpg?1783940670"
        ruling("2012-05-01", "Diregraf Escort can be paired with a Zombie. The protection will have no effect on the pairing.")
    }
}
