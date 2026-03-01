package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Toxin Sliver
 * {3}{B}
 * Creature — Sliver
 * 3/3
 * Whenever a Sliver deals combat damage to a creature, destroy that creature. It can't be regenerated.
 */
val ToxinSliver = card("Toxin Sliver") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Sliver"
    power = 3
    toughness = 3
    oracleText = "Whenever a Sliver deals combat damage to a creature, destroy that creature. It can't be regenerated."

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantTriggeredAbilityToCreatureGroup(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToCreature.event,
                binding = Triggers.DealsCombatDamageToCreature.binding,
                effect = CantBeRegeneratedEffect(EffectTarget.TriggeringEntity) then
                        MoveToZoneEffect(EffectTarget.TriggeringEntity, Zone.GRAVEYARD, byDestruction = true)
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Lars Grant-West"
        flavorText = "It doesn't need to use its venom—it just needs you to know it can."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c04ab6b6-27ee-4c93-a87c-cbc3743f4faf.jpg?1562933654"
    }
}
