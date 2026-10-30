package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Manifold Key — Core Set 2020 #230
 * {1} · Artifact
 *
 * {1}, {T}: Untap another target artifact.
 * {3}, {T}: Target creature can't be blocked this turn.
 */
val ManifoldKey = card("Manifold Key") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Untap another target artifact.\n" +
        "{3}, {T}: Target creature can't be blocked this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val artifact = target(TargetFilter.Artifact.copy(excludeSelf = true))
        effect = Effects.Untap(artifact)
        description = "Untap another target artifact."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        description = "Target creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Lake Hurwitz"
        flavorText = "\"Locks want to be opened, and treasure wants to be free.\"\n—Nayrex Slipfinger, master thief"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/715e637a-dfd8-45a0-b1ea-53e4abd29307.jpg?1783932943"
        ruling("2021-03-19", "Activating Manifold Key's last ability after a creature has become blocked won't cause that creature to become unblocked.")
    }
}
