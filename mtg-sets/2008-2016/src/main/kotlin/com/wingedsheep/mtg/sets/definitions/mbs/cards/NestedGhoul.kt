package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nested Ghoul — Mirrodin Besieged #48
 * {3}{B}{B} · Creature — Phyrexian Zombie Warrior · 4/2
 *
 * Whenever a source deals damage to this creature, create a 2/2 black Phyrexian Zombie creature token.
 *
 * `Triggers.self.isDealtDamage()` fires once per damage event, i.e. once per source dealing damage
 * simultaneously (same trigger Phyrexian Obliterator uses). Reprinted in J22 (Printing row there).
 */
val NestedGhoul = card("Nested Ghoul") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Zombie Warrior"
    power = 4
    toughness = 2
    oracleText = "Whenever a source deals damage to this creature, create a 2/2 black Phyrexian Zombie creature token."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Phyrexian", "Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/4/6/46769d11-a7ae-43ac-8c00-e1681955949b.jpg?1783941356",
        )
        description = "Whenever a source deals damage to this creature, create a 2/2 black Phyrexian Zombie creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Dave Kendall"
        flavorText = "\"The chest cavity is cleared of useless meat. I know just what to do with the space.\"\n—Gyed, Vault Priest"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c035ff58-9df3-4db4-b9d0-97d58080ecfe.jpg?1783941383"
        ruling("2011-06-01", "Nested Ghoul's ability will trigger if a source with infect deals damage to it.")
        ruling(
            "2011-06-01",
            "If multiple sources deal damage to Nested Ghoul simultaneously (because it was blocked by multiple " +
                "creatures, for example), the ability will trigger for each source dealing damage to Nested Ghoul."
        )
    }
}
