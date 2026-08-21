package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Hexhunter
 * {G/W}
 * Creature — Elf Shaman
 * 1 / 1
 *
 * {G/W}, {T}, Sacrifice this creature: Destroy target enchantment.
 *
 * - The printed cost is three atoms in one composite: the hybrid mana, the {T} symbol (so the
 *   ability is summoning-sickness-gated) and sacrificing the Hexhunter itself.
 * - `Costs.SacrificeSelf` rather than a generic sacrifice-a-creature cost — "Sacrifice this
 *   creature" names the source, not a choice.
 * - [Effects.Destroy] lowers to a move-to-graveyard by destruction, so indestructible and
 *   regeneration on the enchantment are honoured (relevant against Ghostly Prison-style
 *   enchantments that have picked up indestructibility).
 * - Modelled directly on Elvish Lyrist (USG), which has the identical ability in mono-green.
 */
val ElvishHexhunter = card("Elvish Hexhunter") {
    manaCost = "{G/W}"
    typeLine = "Creature — Elf Shaman"
    power = 1
    toughness = 1
    oracleText = "{G/W}, {T}, Sacrifice this creature: Destroy target enchantment."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G/W}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target("target", Targets.Enchantment)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"All manner of curses, blights, and luckspoils infect Shadowmoor. We stalk them all, one perilous quest at a time.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2b7f4d7-278b-45fc-98aa-b7c8b9162bcd.jpg?1783942717"
    }
}
