package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Howling Mine — Limited Edition Alpha #247
 * {2} · Artifact
 *
 * At the beginning of each player's draw step, if this artifact is untapped, that player draws an
 * additional card.
 *
 * Each-player draw-step trigger (Dictate of Kruphix's shape) with an intervening "if" (CR 603.4):
 * `interveningIf = Conditions.SourceIsUntapped` is checked when the step begins and again on
 * resolution, so tapping the Mine in response stops the draw. [Player.TriggeringPlayer] is the
 * player whose draw step it is.
 */
val HowlingMine = card("Howling Mine") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of each player's draw step, if this artifact is untapped, " +
        "that player draws an additional card."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        interveningIf = Conditions.SourceIsUntapped
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51f8f6e1-a451-4262-90d3-5107caf54175.jpg?1783948666"
        ruling(
            "2004-10-04",
            "It does not trigger at all if this is tapped at the start of the draw step, and it checks " +
                "this again on resolution."
        )
        ruling(
            "2004-10-04",
            "If Howling Mine leaves the battlefield before it resolves, then the last known tap or " +
                "untap state of the card is used for resolution."
        )
    }
}
