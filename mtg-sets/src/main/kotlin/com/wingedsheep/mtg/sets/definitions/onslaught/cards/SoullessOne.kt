package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Soulless One
 * {3}{B}
 * Creature — Zombie Avatar
 * *|*
 * Trample
 * Soulless One's power and toughness are each equal to the number of Zombies on the battlefield
 * plus the number of Zombie cards in all graveyards.
 */
val SoullessOne = card("Soulless One") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie Avatar"
    oracleText = "Trample\nSoulless One's power and toughness are each equal to the number of Zombies on the battlefield plus the number of Zombie cards in all graveyards."

    dynamicStats(
        DynamicAmount.Add(
            DynamicAmount.CountBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Zombie")),
            DynamicAmount.Count(Player.Each, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype("Zombie"))
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Thomas M. Baxa"
        flavorText = "\"Its residents dig their own graves and crawl in.\"\n—Phage the Untouchable"
        imageUri = "https://cards.scryfall.io/large/front/c/8/c826d786-0d96-4f77-94ae-6907fbce51e0.jpg?1562942286"
    }
}
