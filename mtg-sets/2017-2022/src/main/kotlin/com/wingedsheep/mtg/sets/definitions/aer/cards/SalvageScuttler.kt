package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Salvage Scuttler
 * {4}{U}
 * Creature — Crab
 * 4/4
 * Whenever this creature attacks, return an artifact you control to its owner's hand.
 *
 * Untargeted: the artifact is chosen on resolution (Shrieking Drake's gather → choose → move pipeline).
 */
val SalvageScuttler = card("Salvage Scuttler") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Crab"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature attacks, return an artifact you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Artifact,
                    player = Player.You,
                )
            )
            val returned = chooseExactly(
                1,
                from = candidates,
                prompt = "Return a permanent you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Tony Foti"
        flavorText = "A hermit crab goes through many shells in its lifetime, and not all are natural in origin."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/480be626-cf37-410d-a9c7-e5464345085f.jpg"
        ruling("2017-02-09", "If you control no artifacts, you won’t return anything to your hand. There’s no penalty for being unable to do so.")
    }
}
