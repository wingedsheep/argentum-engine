package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Practiced Tactics — Zendikar Rising #32
 * {W} · Instant
 *
 * Choose target attacking or blocking creature. Practiced Tactics deals damage to that creature
 * equal to twice the number of creatures in your party.
 *
 * The party size is read as the spell resolves and doubled (`partySize() * 2`).
 */
val PracticedTactics = card("Practiced Tactics") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose target attacking or blocking creature. Practiced Tactics deals damage to that " +
        "creature equal to twice the number of creatures in your party. (Your party consists of up to one " +
        "each of Cleric, Rogue, Warrior, and Wizard.)"

    spell {
        val t = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.DealDamage(DynamicAmounts.partySize() * 2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Alex Konstad"
        flavorText = "Each individual has their own crucial role to play."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91b448f4-aa0c-42c7-a771-e8dd20e0520c.jpg?1783929411"
    }
}
