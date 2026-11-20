package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Boulderfall
 * {6}{R}{R}
 * Instant
 * Boulderfall deals 5 damage divided as you choose among any number of targets.
 *
 * Each target must be assigned at least 1 damage, so "any number" caps at five targets (same shape
 * as Pyrotechnics).
 */
val Boulderfall = card("Boulderfall") {
    manaCost = "{6}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Boulderfall deals 5 damage divided as you choose among any number of targets."

    spell {
        target = AnyTarget(count = 5, minCount = 1)
        effect = Effects.DividedDamage(total = 5, minTargets = 1, maxTargets = 5)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Ralph Horsley"
        flavorText = "\"Defeating the Meletians was not so difficult. I needed only to move a mountain.\"\n" +
            "—Eocles, oracle of Purphoros"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66470bac-cce0-4e79-82f9-9fb77aa950eb.jpg?1783939766"

        ruling(
            "2013-09-15",
            "You divide the damage as you cast the spell, not as it resolves. Each target must be assigned " +
                "at least 1 damage."
        )
        ruling(
            "2013-09-15",
            "If some but not all of Boulderfall's targets become illegal, you can't change the division of " +
                "damage. Damage that would have been dealt to illegal targets simply isn't dealt."
        )
        ruling(
            "2013-09-15",
            "If an effect creates a copy of Boulderfall, the division of damage and number of targets can't " +
                "be changed. The effect that creates the copy may allow you to change targets, however."
        )
    }
}
