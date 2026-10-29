package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Lim-Dûl's Vault — Alliances #107 (canonical printing)
 * {U}{B} · Instant
 *
 * Look at the top five cards of your library. As many times as you choose, you may pay 1 life,
 * put those cards on the bottom of your library in any order, then look at the top five cards of
 * your library. Then shuffle and put the last cards you looked at this way on top in any order.
 *
 * Each pass of the do-while loop privately looks at the top five (the look-and-reorder prompt, so
 * nothing is revealed to the opponents) and offers the 1-life payment; paying puts
 * them on the bottom and repeats (Cultivator Colossus's "if you do, repeat this process" shape).
 * `MayPay` only asks while the life is payable (CR 119.4). Declining ends the loop with the last
 * five still on top, so the closing step gathers them, shuffles, and puts them back on top in any
 * order — the same as shuffling the rest beneath them (first ruling).
 */
val LimDulsVault = card("Lim-Dûl's Vault") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Look at the top five cards of your library. As many times as you choose, you may " +
        "pay 1 life, put those cards on the bottom of your library in any order, then look at the " +
        "top five cards of your library. Then shuffle and put the last cards you looked at this way " +
        "on top in any order."

    spell {
        effect = Effects.Pipeline {
            repeatProcess {
                val looked = gather(CardSource.TopOfLibrary(5))
                // A private look (lookAtTopAndReorder's idiom); the order is moot if you then pay.
                toLibraryTop(looked)
                var bottomed: CollectionSlot? = null
                run(Effects.MayPay(
                    cost = Effects.PayLife(1),
                    then = Effects.Pipeline {
                        bottomed = moveTracked(
                            looked,
                            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                            order = CardOrder.ControllerChooses
                        )
                    }
                ))
                // "As many times as you choose" — go round again only after a pass that paid.
                RepeatCondition.WhileCondition(whenMatches(checkNotNull(bottomed)))
            }
            val last = gather(CardSource.TopOfLibrary(5))
            run(Effects.ShuffleLibrary())
            toLibraryTop(last)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9b0164c-2d4e-48ab-addd-322d9b504739.jpg?1783947166"
        ruling("2007-09-16", "In other words: — Step 1: Look at the top five cards of your library. — Step 2: If you like them, proceed to step 3a. If you don't like them, proceed to step 3b. — Step 3a: Shuffle the rest of your library, then put those five cards back on top of your library in the order you want. The spell has finished resolving. — Step 3b: Put those five cards on the bottom of your library in the order you want. Pay 1 life. Return to step 1.")
        ruling("2007-09-16", "If you pay no life, you shuffle the rest of your library and then put the top five cards back on top in any order.")
        ruling("2007-09-16", "If you have five or fewer cards in your library, you can pay 1 life as many times as you like, but you'll keep seeing the same cards.")
    }
}
