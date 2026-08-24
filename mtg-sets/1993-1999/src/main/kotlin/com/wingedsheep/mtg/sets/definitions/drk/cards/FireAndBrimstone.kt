package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.PlayerAttackedWithCreaturesThisTurn
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Fire and Brimstone
 * {3}{W}{W}
 * Instant
 * Fire and Brimstone deals 4 damage to target player who attacked this turn and 4 damage to you.
 *
 * "Target player who attacked this turn" is a restriction on the *candidate*, not a condition on the
 * spell: only players who declared an attacker this turn are legal targets at all, and CR 608.2b
 * re-checks that at resolution. `Player.Candidate` is the binding the engine supplies while
 * enumerating and re-validating each candidate.
 *
 * The 4 damage to you is not conditional on the first half landing — the spell deals both, so it is
 * a plain second sub-effect rather than something gated on the target surviving.
 */
val FireAndBrimstone = card("Fire and Brimstone") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Fire and Brimstone deals 4 damage to target player who attacked this turn and " +
        "4 damage to you."

    spell {
        val attacker = target(
            "target player who attacked this turn",
            TargetPlayer(
                restriction = PlayerAttackedWithCreaturesThisTurn(
                    player = Player.Candidate,
                    filter = GameObjectFilter.Creature,
                    atLeast = 1,
                ),
                descriptionOverride = "target player who attacked this turn",
            ),
        )
        effect = Effects.Composite(
            Effects.DealDamage(4, attacker),
            Effects.DealDamage(4, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5208dbb-63d2-4789-8ef9-f82499a43b3a.jpg?1783947948"
    }
}
