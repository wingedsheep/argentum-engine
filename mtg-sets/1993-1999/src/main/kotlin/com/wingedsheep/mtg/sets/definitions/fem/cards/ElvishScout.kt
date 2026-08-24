package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Elvish Scout
 * {G}
 * Creature — Elf Scout
 * 1/1
 * {G}, {T}: Untap target attacking creature you control. Prevent all combat damage that would be
 * dealt to and dealt by it this turn.
 *
 * Untapping an attacker does not remove it from combat (CR 506.4) — it simply stops the creature
 * being tapped, and the prevention shield makes the exchange damageless in both directions.
 */
val ElvishScout = card("Elvish Scout") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    oracleText = "{G}, {T}: Untap target attacking creature you control. Prevent all combat " +
        "damage that would be dealt to and dealt by it this turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val t = target(
            "target attacking creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.attacking().youControl()))
        )
        effect = Effects.Composite(
            Effects.Untap(t),
            Effects.PreventCombatDamageToAndBy(t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68a"
        artist = "Mark Poole"
        flavorText = "Even one whose ears were closely tuned to the sounds of Havenwood could miss hearing a Scout move past."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/689cd2ed-be81-4769-a8ec-287946301396.jpg?1783947888"
    }
}
