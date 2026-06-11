package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect

/**
 * Karai's Technique
 * {1}{W}{B}
 * Sorcery
 *
 * Sneak {W}{B} (You may cast this spell for {W}{B} if you also return an
 * unblocked attacker you control to hand during the declare blockers step.)
 * Choose one or both —
 * • Target creature gets +3/+3 until end of turn.
 * • Target creature gets -3/-3 until end of turn.
 */
val KaraisTechnique = card("Karai's Technique") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Sneak {W}{B} (You may cast this spell for {W}{B} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nChoose one or both —\n• Target creature gets +3/+3 until end of turn.\n• Target creature gets -3/-3 until end of turn."

    sneak("{W}{B}")

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Target creature gets +3/+3 until end of turn") {
                val c = target("target creature", Targets.Creature)
                effect = ModifyStatsEffect(3, 3, c, Duration.EndOfTurn)
            }
            mode("Target creature gets -3/-3 until end of turn") {
                val c = target("target creature", Targets.Creature)
                effect = ModifyStatsEffect(-3, -3, c, Duration.EndOfTurn)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/037fe188-3355-4724-b2eb-e2448fc55607.jpg?1771587021"
    }
}
