package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Seedcradle Witch
 * {G/W}
 * Creature — Elf Shaman
 * 1 / 1
 *
 * {2}{G}{W}: Target creature gets +3/+3 until end of turn. Untap that creature.
 *
 * - "That creature" in the second sentence is the same object as the first sentence's target, so
 *   both halves reference the one named target rather than declaring a second one.
 * - Ordered composite: the pump resolves before the untap, matching the printed sentence order.
 *   The untap is unconditional — it happens even if the creature is already untapped.
 * - The activation cost is the printed hybrid-free `{2}{G}{W}`; only the card's own mana cost is
 *   hybrid.
 */
val SeedcradleWitch = card("Seedcradle Witch") {
    manaCost = "{G/W}"
    typeLine = "Creature — Elf Shaman"
    power = 1
    toughness = 1
    oracleText = "{2}{G}{W}: Target creature gets +3/+3 until end of turn. Untap that creature."

    activatedAbility {
        cost = Costs.Mana("{2}{G}{W}")
        val creature = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(3, 3, creature),
            Effects.Untap(creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Steven Belledin"
        flavorText = "She whispered a prayer for strength, and her wishes wafted away like seeds on the wind."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0ae8525-ce10-40bd-8980-a05fb81a0fac.jpg?1783942714"
    }
}
