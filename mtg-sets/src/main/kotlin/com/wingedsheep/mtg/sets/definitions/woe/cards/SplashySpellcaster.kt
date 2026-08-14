package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Splashy Spellcaster
 * {3}{U}
 * Creature — Elemental Wizard
 * 2/4
 *
 * Whenever you cast an instant or sorcery spell, create a Sorcerer Role token attached to up to
 * one other target creature you control.
 *
 * "Up to one **other** target creature you control" is [TargetFilter.OtherCreatureYouControl] with
 * `optional = true`: the Spellcaster can't Role itself, and declining the target is legal. Per the
 * card's ruling, declining means no token is created at all — which falls out of
 * [Effects.CreateRoleToken] resolving against an unset target. The one-Role-per-creature
 * state-based action (replace an existing Role you control on that creature) lives behind
 * `CreateRoleTokenEffect`, so it isn't re-modelled here.
 */
val SplashySpellcaster = card("Splashy Spellcaster") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Wizard"
    oracleText = "Whenever you cast an instant or sorcery spell, create a Sorcerer Role token " +
        "attached to up to one other target creature you control. (If you control another Role on it, " +
        "put that one into the graveyard. Enchanted creature gets +1/+1 and has \"Whenever this " +
        "creature attacks, scry 1.\")"
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        val t = target(
            "up to one other target creature you control",
            TargetCreature(filter = TargetFilter.OtherCreatureYouControl, optional = true)
        )
        effect = Effects.CreateRoleToken("Sorcerer Role", t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73ebd7f0-a54d-43a8-a5ee-9d6835308794.jpg?1783915114"
        ruling("2023-09-01", "If you don't choose a target for Splashy Spellcaster's ability, the Sorcerer Role token won't be created.")
        ruling("2023-09-01", "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and the enchant creature ability.")
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, each of " +
                "those Roles except the one with the most recent timestamp is put into its owner's graveyard. " +
                "This is a state-based action."
        )
    }
}
