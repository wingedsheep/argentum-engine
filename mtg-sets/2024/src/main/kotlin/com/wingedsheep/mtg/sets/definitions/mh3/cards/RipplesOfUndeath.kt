package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Ripples of Undeath — Modern Horizons 3 #107
 * {1}{B} · Enchantment · Rare
 *
 * At the beginning of your first main phase, mill three cards. Then you may pay {1} and 3 life.
 * If you do, put a card from among those cards into your hand.
 *
 * The mill is a mill-flagged top-of-library gather moved to the graveyard with `moveTracked`, so
 * "those cards" is only what actually landed in the graveyard (a replacement such as Rest in
 * Peace leaves nothing to take). The payment is one [Effects.MayPay] gate over the composite cost
 * `{1}` then 3 life — only offered when both halves are affordable (the Zoraline shape) — and its
 * payoff picks exactly one of the milled cards into hand.
 */
val RipplesOfUndeath = card("Ripples of Undeath") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your first main phase, mill three cards. Then you may pay {1} " +
        "and 3 life. If you do, put a card from among those cards into your hand."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(3, isMill = true))
            val milled = moveTracked(top, CardDestination.ToZone(Zone.GRAVEYARD), name = "milled")
            run(
                Effects.MayPay(
                    cost = Effects.PayMana("{1}") then Effects.PayLife(3),
                    then = Effects.Pipeline {
                        val chosen = chooseExactly(
                            1,
                            from = milled,
                            prompt = "Put a card from among the milled cards into your hand",
                            selectedLabel = "Put in hand",
                            remainderLabel = "Leave in graveyard"
                        )
                        toHand(chosen)
                    },
                    descriptionOverride = "Pay {1} and 3 life to put a card from among the milled " +
                        "cards into your hand?"
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Ben Wootten"
        flavorText = "As the monster lay dying, it stared into the eyes of the human who slew it. " +
            "Decades later, it has yet to break its gaze."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a201d1bc-e3fe-4f59-bd48-3683996ac308.jpg?1783911276"
    }
}
