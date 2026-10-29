package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Enthralling Hold
 * {3}{U}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * You can't choose an untapped creature as this spell's target as you cast it.
 * You control enchanted creature.
 *
 * The Dream Leash shape: [auraTarget] stays the printed "enchant creature", and [auraCastTarget]
 * narrows only the cast-time choice to a tapped creature — so the spell still resolves if the
 * creature untaps first, and a non-cast attach may pick an untapped creature (2020-06-23 ruling).
 */
val EnthrallingHold = card("Enthralling Hold") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "You can't choose an untapped creature as this spell's target as you cast it.\n" +
        "You control enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)
    auraCastTarget = TargetObject(filter = TargetFilter.Creature.tapped())

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Colin Boyer"
        flavorText = "\"I offered to pay him to do the job, but he refused. So I took matters into my own hands.\"\n—Tahim, the Puppetmaster"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3bc7176-abe0-47cf-a242-cf22a1f590be.jpg?1783930728"
        ruling("2020-06-23", "The middle ability of Enthralling Hold affects only the choice of target as the spell is cast. If the creature becomes untapped before the spell resolves, it still resolves. If a player is allowed to change the spell's target while it's on the stack, they may choose an untapped creature. If you put Enthralling Hold onto the battlefield without casting it, you may attach it to an untapped creature.")
        ruling("2020-06-23", "Gaining control of a permanent doesn't cause you to gain control of any Auras or Equipment attached to it. They'll remain attached, but an Aura's effect that affects \"you\" still affects its controller rather than you, the controller of an Equipment can move it during their next main phase, and so on.")
    }
}
