package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Phyrexian Reaper
 * {4}{B}
 * Creature — Phyrexian Zombie
 * 3/3
 * Whenever this creature becomes blocked by a green creature, destroy that creature.
 * It can't be regenerated.
 *
 * "That creature" is the green blocker (not a target). The becomes-blocked SELF trigger
 * with a [Filters.GreenCreature] filter fires once per matching blocker, exposing the
 * blocker as [EffectTarget.TriggeringEntity].
 */
val PhyrexianReaper = card("Phyrexian Reaper") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Zombie"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature becomes blocked by a green creature, destroy that creature. It can't be regenerated."

    triggeredAbility {
        trigger = Triggers.becomesBlocked(filter = Filters.GreenCreature, binding = TriggerBinding.SELF)
        effect = CantBeRegeneratedEffect(EffectTarget.TriggeringEntity) then
                Effects.Move(EffectTarget.TriggeringEntity, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Sam Wood"
        flavorText = "It desires only to help others shed the itchy wet skin of life."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccdd498b-1081-43fe-8193-518337a5a3ea.jpg?1562936179"
    }
}
