package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flaying Tendrils
 * {1}{B}{B}
 * Sorcery
 * Devoid (This card has no color.)
 * All creatures get -2/-2 until end of turn. If a creature would die this turn, exile it instead.
 *
 * Malicious Malfunction's shape: the exile clause is a turn-long replacement keyed on *any*
 * creature going battlefield → graveyard, not a per-creature mark, so it also catches creatures
 * that weren't on the battlefield (or weren't creatures) as the spell resolved (ruling). Only the
 * creatures on the battlefield at resolution get -2/-2; the replacement is installed first, and
 * state-based actions check deaths only after the spell has finished resolving anyway.
 */
val FlayingTendrils = card("Flaying Tendrils") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "All creatures get -2/-2 until end of turn. If a creature would die this turn, exile it instead."

    keywords(Keyword.DEVOID)

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
        collectorNumber = "70"
        artist = "Slawomir Maniak"
        flavorText = "Only one Eldrazi stood against them, yet the defenders were outnumbered."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77899cb2-4d87-4c2d-99ae-1ae75bc5dc86.jpg?1783937915"
        ruling("2016-01-22", "Only creatures that are on the battlefield as Flaying Tendrils resolves will get -2/-2. However, any creature that would die that turn will be exiled, even if it wasn’t on the battlefield or wasn’t a creature as Flaying Tendrils resolved.")
        ruling("2016-01-22", "Creatures that Flaying Tendrils would cause to die will be exiled instead. The game doesn’t check to see which creatures die until after Flaying Tendrils has finished resolving.")
    }
}
