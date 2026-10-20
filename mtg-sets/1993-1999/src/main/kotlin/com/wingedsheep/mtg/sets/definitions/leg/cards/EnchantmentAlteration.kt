package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enchantment Alteration
 * {U}
 * Instant
 *
 * Attach target Aura attached to a creature or land to another permanent of that type.
 *
 * Only the Aura is targeted; the new host is chosen as the spell resolves, among the permanents the
 * Aura could legally enchant, never its current host. "That type" is read from the Aura's host at
 * resolution: a creature host sends it to another creature, a land host to another land, and a host
 * that is both (Dryad Arbor, an animated land) is a permanent of either type, so either is offered.
 */
val EnchantmentAlteration = card("Enchantment Alteration") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Attach target Aura attached to a creature or land to another permanent of that type."

    spell {
        val aura = target(
            TargetFilter(
                GameObjectFilter.Enchantment.withSubtype("Aura").attachedTo(GameObjectFilter.CreatureOrLand)
            )
        )
        val onCreature = Conditions.TargetMatchesFilter(
            GameObjectFilter.Enchantment.attachedTo(GameObjectFilter.Creature), aura
        )
        val onLand = Conditions.TargetMatchesFilter(
            GameObjectFilter.Enchantment.attachedTo(GameObjectFilter.Land), aura
        )
        effect = Effects.If(
            condition = onCreature,
            then = Effects.If(
                condition = onLand,
                then = Effects.AttachToChosenHost(aura, GameObjectFilter.CreatureOrLand),
                otherwise = Effects.AttachToChosenHost(aura, GameObjectFilter.Creature)
            ),
            otherwise = Effects.AttachToChosenHost(aura, GameObjectFilter.Land),
            descriptionOverride = "Attach target Aura attached to a creature or land to another permanent of that type."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf52f8a0-d027-47f1-bb91-508ef1a74409.jpg?1783948077"
    }
}
