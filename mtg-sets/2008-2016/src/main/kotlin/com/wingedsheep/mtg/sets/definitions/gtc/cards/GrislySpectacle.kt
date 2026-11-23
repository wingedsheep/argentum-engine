package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grisly Spectacle
 * {2}{B}{B}
 * Instant
 * Destroy target nonartifact creature. Its controller mills cards equal to that creature's power.
 *
 * The mill count is the creature's power as it last existed on the battlefield (ruling
 * 2017-11-17). A spell's target is read live, with no last-known fallback once it leaves play, so
 * the mill is sequenced before the destroy (Crumble / Agonizing Demise's shape): the power —
 * anthems and counters included — and its controller are read while it is still on the
 * battlefield, and the amount is fixed before any card moves.
 */
val GrislySpectacle = card("Grisly Spectacle") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target nonartifact creature. Its controller mills cards equal to that creature's power."

    spell {
        val creature = target(TargetFilter.Creature.nonartifact())
        effect = Patterns.Library.mill(
            DynamicAmounts.powerOf(creature),
            EffectTarget.PlayerRef(Player.ControllerOf("target creature"))
        ) then
            Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Zoltan Boros"
        flavorText = "\"Watch people flock to a murder scene. Then tell me we're not all a little sick in the head.\"\n" +
            "—Juri, proprietor of the Juri Revue"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c26d0f6e-e7bd-4206-a0da-1c9c203a73f2.jpg?1783940131"
        ruling(
            "2017-11-17",
            "Use the creature's power the last time it was on the battlefield to determine how many cards its controller puts into their graveyard.",
        )
        ruling(
            "2017-11-17",
            "If the creature is an illegal target when Grisly Spectacle tries to resolve, it won't resolve and none of its effects will happen. The creature's controller won't put any cards into their graveyard.",
        )
    }
}
