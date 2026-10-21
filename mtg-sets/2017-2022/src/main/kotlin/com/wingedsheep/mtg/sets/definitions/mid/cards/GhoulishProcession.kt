package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ghoulish Procession
 * {1}{B}
 * Enchantment
 * Whenever one or more nontoken creatures die, create a 2/2 black Zombie creature token with
 * decayed. This ability triggers only once each turn.
 *
 * The batched death trigger (`Triggers.oneOrMore(filter.anyController()).die()` over nontoken creatures) fires at
 * most once per death batch (CR 603.3b) and is further capped to once per turn (`oncePerTurn`).
 * The token is created with the Decayed keyword (`keywords`), which the engine realizes as "can't
 * block" + "when it attacks, sacrifice it at end of combat" (CR 702.147a).
 */
val GhoulishProcession = card("Ghoulish Procession") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more nontoken creatures die, create a 2/2 black Zombie creature " +
        "token with decayed. This ability triggers only once each turn. (A creature with decayed " +
        "can't block. When it attacks, sacrifice it at end of combat.)"

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.nontoken().anyController()).die()
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
        )
        description = "Whenever one or more nontoken creatures die, create a 2/2 black Zombie creature " +
            "token with decayed."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Vincent Proce"
        flavorText = "It was a family reunion to die for."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/363bd6a6-e8e4-4626-a43c-0b77339dd28b.jpg?1782703666"
    }
}
