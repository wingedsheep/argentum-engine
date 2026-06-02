package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Phyrexian Slayer
 * {3}{B}
 * Creature — Phyrexian Minion
 * 2/2
 * Flying
 * Whenever this creature becomes blocked by a white creature, destroy that creature.
 * It can't be regenerated.
 *
 * "That creature" is the white blocker (not a target). The becomes-blocked SELF trigger
 * with a white-creature filter fires once per matching blocker, exposing the blocker as
 * [EffectTarget.TriggeringEntity]. Mirrors Phyrexian Reaper (green blocker variant).
 */
val PhyrexianSlayer = card("Phyrexian Slayer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Minion"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever this creature becomes blocked by a white creature, destroy that creature. It can't be regenerated."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.becomesBlocked(
            filter = GameObjectFilter.Creature.withColor(Color.WHITE),
            binding = TriggerBinding.SELF,
        )
        effect = CantBeRegeneratedEffect(EffectTarget.TriggeringEntity) then
                Effects.Move(EffectTarget.TriggeringEntity, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Sam Wood"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fa8c604-343f-4c94-ac25-439ab1845c19.jpg?1562914368"
    }
}
