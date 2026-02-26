package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dripping Dead
 * {4}{B}{B}
 * Creature — Zombie
 * 4/1
 * Dripping Dead can't block.
 * Whenever Dripping Dead deals combat damage to a creature, destroy that creature. It can't be regenerated.
 */
val DrippingDead = card("Dripping Dead") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 1
    oracleText = "Dripping Dead can't block.\nWhenever Dripping Dead deals combat damage to a creature, destroy that creature. It can't be regenerated."

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToCreature
        effect = CantBeRegeneratedEffect(EffectTarget.TriggeringEntity) then
                MoveToZoneEffect(EffectTarget.TriggeringEntity, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Thomas M. Baxa"
        flavorText = "It oozes death from every pore."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdb3b483-01a8-4f54-9a3a-0d3f5aa3cd8b.jpg?1562936359"
    }
}
