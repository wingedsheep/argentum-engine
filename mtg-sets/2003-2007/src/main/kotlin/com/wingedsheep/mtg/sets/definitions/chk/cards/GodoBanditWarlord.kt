package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Godo, Bandit Warlord
 * {5}{R}
 * Legendary Creature — Human Barbarian
 * 3/3
 *
 * When Godo enters, you may search your library for an Equipment card, put it onto the
 * battlefield, then shuffle.
 * Whenever Godo attacks for the first time each turn, untap it and all Samurai you control.
 * After this phase, there is an additional combat phase.
 *
 * "Untap it" is Godo itself; "all Samurai you control" is every Samurai *permanent* (creature
 * or not). Per the 2020-08-07 ruling the extra phase is combat only — no additional main phase —
 * which is exactly the [Effects.AddCombatPhase] atom.
 */
val GodoBanditWarlord = card("Godo, Bandit Warlord") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Barbarian"
    oracleText = "When Godo enters, you may search your library for an Equipment card, put it onto the battlefield, then shuffle.\n" +
        "Whenever Godo attacks for the first time each turn, untap it and all Samurai you control. After this phase, there is an additional combat phase."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.FirstTimeEachTurn))
        effect = Effects.Untap(EffectTarget.Self) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Any.withSubtype(Subtype.SAMURAI).youControl()),
                Effects.Untap(EffectTarget.IterationEntity)
            ) then
            Effects.AddCombatPhase
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcff0b92-edd0-4197-965b-3fd86bc884d8.jpg?1783944299"
        ruling(
            "2020-08-07",
            "Unlike many effects that grant additional combat phases, you don't get an additional main phase with Godo, Bandit Warlord's ability. The additional combat phase happens immediately after the first combat phase."
        )
    }
}
