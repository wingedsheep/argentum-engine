package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SuppressEntersTriggers

/**
 * Torpor Orb — New Phyrexia #162 (canonical printing)
 * {2} · Artifact
 *
 * Creatures entering don't cause abilities to trigger.
 *
 * The reusable [SuppressEntersTriggers] static (filter = creatures). The engine removes, at
 * trigger-detection time, every enters-the-battlefield trigger caused by a creature entering —
 * both the creature's own "When this enters …" and other permanents' "Whenever a creature
 * enters …" abilities that fired off that entry — for any player. Leaves/dies and other
 * triggers, and enters-tapped / enters-with-counters replacements, are unaffected.
 */
val TorporOrb = card("Torpor Orb") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Creatures entering don't cause abilities to trigger."

    staticAbility {
        ability = SuppressEntersTriggers(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Svetlin Velinov"
        flavorText = "\"Phyrexia is certainly dangerous, but I have to admire some of its innovations.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/953610f6-ea96-4e71-969f-50ecac09c091.jpg?1722108794"
    }
}
