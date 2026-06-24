package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kitsune's Technique
 * {4}{U}{U}
 * Instant
 *
 * Sneak {1}{U} (You may cast this spell for {1}{U} if you also return an unblocked attacker
 * you control to hand during the declare blockers step.)
 * Target opponent mills half their library, rounded up.
 *
 * "Half their library, rounded up" is a [DynamicAmount.Divide] of the target opponent's library
 * size ([DynamicAmount.Count] over [Zone.LIBRARY] for the first context player) by two with
 * `roundUp = true`. `Patterns.Library.mill` takes the dynamic count and the target opponent
 * ([Targets.Opponent], bound as context target 0), gathering that many cards from the top of
 * their library into their graveyard.
 */
val KitsunesTechnique = card("Kitsune's Technique") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Sneak {1}{U} (You may cast this spell for {1}{U} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nTarget opponent mills half their library, rounded up."

    sneak("{1}{U}")

    spell {
        val opponent = target("target opponent", Targets.Opponent)
        effect = Patterns.Library.mill(
            count = DynamicAmount.Divide(
                numerator = DynamicAmount.Count(Player.ContextPlayer(0), Zone.LIBRARY),
                denominator = DynamicAmount.Fixed(2),
                roundUp = true
            ),
            target = opponent
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Rose Benjamin"
        flavorText = "\"Like a monster seeking prey in the night, the truth will hunt you down.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ca5327e-df90-483c-875f-73a23781f56d.jpg?1769005745"
    }
}
