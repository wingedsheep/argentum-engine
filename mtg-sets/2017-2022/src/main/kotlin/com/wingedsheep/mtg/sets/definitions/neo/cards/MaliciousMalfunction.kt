package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malicious Malfunction
 * {1}{B}{B}
 * Sorcery
 * All creatures get -2/-2 until end of turn. If a creature would die this turn, exile it instead.
 *
 * Malicious Eclipse's shape with every creature in the replacement: a floating grant keyed on the
 * caster, installed before the -2/-2 so it is in place when state-based actions check the kills, and
 * covering creatures that die later in the turn too.
 */
val MaliciousMalfunction = card("Malicious Malfunction") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -2/-2 until end of turn. If a creature would die this turn, exile it instead."

    spell {
        effect = Effects.GrantReplacementEffect(
            replacement = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature,
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD
                )
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn
        ) then
            Effects.ForEachInGroup(
                filter = GroupFilter.AllCreatures,
                effect = Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Deruchenko Alexander"
        flavorText = "\"That? A minor test, nothing more. Incompleat flesh is weak.\"\n—Jin-Gitaxias"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56e7415f-f014-4ece-81db-d8271444d9e9.jpg?1783923881"
    }
}
