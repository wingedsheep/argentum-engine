package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sphinx of the Second Sun
 * {6}{U}{U}
 * Creature — Sphinx
 * 6/6
 * Flying
 * At the beginning of each of your postcombat main phases, there is an additional beginning phase
 * after this phase. (The beginning phase includes the untap, upkeep, and draw steps.)
 *
 * The trigger and [Effects.AddBeginningPhase] are Shadow of the Second Sun's, keyed to you.
 */
val SphinxOfTheSecondSun = card("Sphinx of the Second Sun") {
    manaCost = "{6}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "At the beginning of each of your postcombat main phases, there is an additional beginning " +
        "phase after this phase. (The beginning phase includes the untap, upkeep, and draw steps.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        effect = Effects.AddBeginningPhase
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "99"
        artist = "Antonio José Manzanedo"
        flavorText = "He watches in silence as the shadows shift, bringing Amonkhet ever nearer to the Hour of Revelation."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e68b70a6-a150-4d81-921c-9b178fe0037d.jpg?1783928849"
        ruling(
            "2020-11-10",
            "The additional beginning phase all happens during the current turn. Any effects that last " +
                "\"until your next turn\" or similar won't expire just because you'll go through an " +
                "additional beginning phase."
        )
        ruling(
            "2020-11-10",
            "Even if you don't attack with any creatures during combat, you'll still get a postcombat main " +
                "phase and Sphinx of the Second Sun's ability will still trigger."
        )
        ruling(
            "2020-11-10",
            "If multiple phases are added to the same point in your turn, the most recently created phase " +
                "happens first."
        )
    }
}
