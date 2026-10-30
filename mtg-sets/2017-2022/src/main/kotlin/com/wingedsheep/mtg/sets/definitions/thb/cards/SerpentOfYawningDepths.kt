package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Serpent of Yawning Depths
 * {4}{U}{U}
 * Enchantment Creature — Serpent
 * 6/6
 * Krakens, Leviathans, Octopuses, and Serpents you control can't be blocked except by Krakens,
 * Leviathans, Octopuses, and Serpents.
 *
 * Shifting Sliver's group-scoped [CantBeBlockedExceptBy], with the affected group narrowed to sea
 * monsters you control and both sides matching any of the four creature types.
 */
val SerpentOfYawningDepths = card("Serpent of Yawning Depths") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Serpent"
    power = 6
    toughness = 6
    oracleText = "Krakens, Leviathans, Octopuses, and Serpents you control can't be blocked except by " +
        "Krakens, Leviathans, Octopuses, and Serpents."

    val seaMonster = GameObjectFilter.Creature.withAnySubtype("Kraken", "Leviathan", "Octopus", "Serpent")
    staticAbility {
        ability = CantBeBlockedExceptBy(
            blockerFilter = seaMonster,
            filter = GroupFilter(seaMonster.youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "291"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Gleams like moonlight on water drew us through the storm and toward our doom.\"\n" +
            "—Siona, captain of the *Pyleas*"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ac18ae-0d1e-43f5-9cc9-722fa6e36fe5.jpg?1783931490"
    }
}
