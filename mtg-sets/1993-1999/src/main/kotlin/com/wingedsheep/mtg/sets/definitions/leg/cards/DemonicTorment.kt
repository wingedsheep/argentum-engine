package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Demonic Torment
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack.
 * Prevent all combat damage that would be dealt by enchanted creature.
 *
 * Pacifism's attack lock scoped to the host, plus Sandskin's "dealt by enchanted creature"
 * combat-damage prevention half.
 */
val DemonicTorment = card("Demonic Torment") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't attack.\n" +
        "Prevent all combat damage that would be dealt by enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.attachedToBySource(),
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3ec14bc-95e9-47ce-b51e-d5eac9b345fe.jpg?1783948067"
    }
}
