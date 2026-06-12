package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Witch-king, Bringer of Ruin
 * {4}{B}{B}
 * Legendary Creature — Wraith Noble
 * 5/3
 *
 * Flying
 * Whenever Witch-king attacks, defending player sacrifices a creature with the least power among
 * creatures they control.
 *
 * Gap 36 (least-power filter): adds `StatePredicate.HasLeastPower` (mirror of the existing
 * `HasGreatestPower`) + `GameObjectFilter.Creature.hasLeastPower()`. The edict reuses
 * `Effects.Sacrifice(filter, 1, defendingPlayer)`. Per the engine's attack-trigger convention
 * (Agate Blade Assassin), "defending player" is modeled as `Player.EachOpponent` (the defending
 * player in a two-player game; no targeting, matching the oracle's non-"target" wording).
 */
val WitchKingBringerOfRuin = card("Witch-king, Bringer of Ruin") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Wraith Noble"
    power = 5
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever Witch-king attacks, defending player sacrifices a creature with the least power " +
        "among creatures they control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.hasLeastPower(),
            1,
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "293"
        artist = "Denman Rooke"
        flavorText = "\"No living man may hinder me!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90da1d3e-dbcb-4b1e-a606-d4fc1a60a8fe.jpg?1687424793"
    }
}
