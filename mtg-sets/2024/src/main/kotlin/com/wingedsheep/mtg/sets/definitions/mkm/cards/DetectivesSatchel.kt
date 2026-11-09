package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Detective's Satchel — Murders at Karlov Manor #196
 * {2}{U}{R} · Artifact · Uncommon
 *
 * When this artifact enters, investigate twice.
 * {T}: Create a 1/1 colorless Thopter artifact creature token with flying. Activate only if you've
 * sacrificed an artifact this turn.
 *
 * The Satchel is its own enabler: the two Clues it makes on entry are artifacts, and sacrificing
 * one to draw sets the gate for the Thopter. That gate is
 * [Conditions.SacrificedArtifactThisTurn] — the per-turn sacrifice record filtered to artifacts, the same one
 * behind Suspicious Detonation and Furtive Courier — wrapped in
 * [ActivationRestriction.OnlyIfCondition] so the tap ability simply isn't offered until it holds.
 *
 * Turn history, not a board scan: the sacrificed artifact having since left the graveyard doesn't
 * clear it, an opponent sacrificing their own artifact never sets it, and it resets at cleanup —
 * so the Satchel makes at most one Thopter per turn in practice only because it taps for it, not
 * because the tracker is spent.
 */
val DetectivesSatchel = card("Detective's Satchel") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, investigate twice. (To investigate, create a Clue " +
        "token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "{T}: Create a 1/1 colorless Thopter artifact creature token with flying. Activate only " +
        "if you've sacrificed an artifact this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Investigate(2)
        description = "When this artifact enters, investigate twice."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            name = "Thopter",
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SacrificedArtifactThisTurn)
        )
        description = "{T}: Create a 1/1 colorless Thopter artifact creature token with flying. " +
            "Activate only if you've sacrificed an artifact this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Andrew Mar"
        flavorText = "It's a crime lab in a bag."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c05bf2d-7d4f-4717-b1ea-ec4284854f4f.jpg?1783912852"
    }
}
