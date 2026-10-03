package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Presence of Gond
 * {2}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{T}: Create a 1/1 green Elf Warrior creature token."
 *
 * The Malicious Intent shape: [GrantActivatedAbility] hands the quoted ability to the enchanted
 * creature, so the {T} is the creature's own tap (summoning sickness applies) and the creature's
 * controller activates it and gets the token.
 */
val PresenceOfGond = card("Presence of Gond") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{T}: Create a 1/1 green Elf Warrior creature token.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Elf", "Warrior")
                )
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Brandon Kitkouski"
        flavorText = "\"Here lies Gond, hero of Safehold Taldwen. May he ever guide our quest.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/546c647d-8d0a-4b88-804e-ce0348eb772a.jpg"
    }
}
