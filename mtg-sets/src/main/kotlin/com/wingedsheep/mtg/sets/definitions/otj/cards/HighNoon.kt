package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * High Noon — Outlaws of Thunder Junction #15
 * {1}{W} · Enchantment · Rare
 *
 * Each player can't cast more than one spell each turn.
 * {4}{R}, Sacrifice this enchantment: It deals 5 damage to any target.
 *
 * The cast restriction is the global ([eachPlayer]) variant of [RestrictSpellsCastPerTurn] —
 * it binds every player, not just High Noon's controller. The activated ability sacrifices
 * the enchantment and deals 5 damage to any target, with the enchantment itself as the damage
 * source ([EffectTarget.Self] is captured as last-known information when the source leaves the
 * battlefield as part of paying the cost).
 *
 * Scryfall rulings (2024-04-12): casting High Noon itself counts as a spell cast that turn, so
 * its controller can cast no further spell that turn; a countered spell still counts toward the
 * one-spell cap — both handled by the engine's spells-cast-this-turn tally.
 */
val HighNoon = card("High Noon") {
    manaCost = "{1}{W}"
    colorIdentity = "WR"
    typeLine = "Enchantment"
    oracleText = "Each player can't cast more than one spell each turn.\n" +
        "{4}{R}, Sacrifice this enchantment: It deals 5 damage to any target."

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1, eachPlayer = true)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{R}"), Costs.SacrificeSelf)
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.DealDamage(5, anyTarget, damageSource = EffectTarget.Self)
        description = "{4}{R}, Sacrifice this enchantment: It deals 5 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Eduardo Francisco"
        flavorText = "They both knew only one of them would walk away."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9995e0e6-7c9c-4fef-8fd2-8fb1622e6ec8.jpg?1712355285"
    }
}
