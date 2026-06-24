package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Shark Shredder, Killer Clone
 * {2}{B}{B}
 * Legendary Creature — Shark Octopus Ninja
 * 4/4
 *
 * Sneak {3}{B}{B}
 * First strike
 * Whenever Shark Shredder deals combat damage to a player, put up to one target creature card
 * from that player's graveyard onto the battlefield under your control. It enters tapped and
 * attacking that player.
 *
 * The trigger targets up to one creature card in the damaged player's graveyard ([TargetObject]
 * with `optional = true`, scoped to [Zone.GRAVEYARD]) and reanimates it via `Effects.Move`
 * to the battlefield with `controllerOverride = Controller` (under your control) and
 * `ZonePlacement.TappedAndAttacking` (tapped and attacking the defending player) — the same
 * tapped-and-attacking placement Raph & Mikey and the Sneak pipeline use.
 *
 * Scoping caveat: the engine has no "owned by the triggering player" target predicate, so
 * "that player's graveyard" is expressed as `ownedByOpponent()`. In the engine's two-player
 * games the damaged player IS the lone opponent, so this is exact; only in hypothetical
 * multiplayer could it let you reach a different opponent's graveyard.
 */
val SharkShredderKillerClone = card("Shark Shredder, Killer Clone") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Shark Octopus Ninja"
    oracleText = "Sneak {3}{B}{B}\nFirst strike\nWhenever Shark Shredder deals combat damage to a player, put up to one target creature card from that player's graveyard onto the battlefield under your control. It enters tapped and attacking that player."
    power = 4
    toughness = 4

    keywords(Keyword.FIRST_STRIKE)

    sneak("{3}{B}{B}")

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val creatureCard = target(
            "up to one target creature card in that player's graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature.ownedByOpponent(), zone = Zone.GRAVEYARD)
            )
        )
        effect = Effects.Move(
            target = creatureCard,
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.TappedAndAttacking,
            controllerOverride = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f946b3d-7d2a-4210-a909-d16078757e3b.jpg?1769006593"
    }
}
