package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Find Some Prisoners
 * {1}{R}
 * Instant
 *
 * Choose one —
 * • Break Their Chains — Destroy target artifact.
 * • Interrogate Them — Exile the top three cards of target opponent's library. Choose one of them.
 *   Until the end of your next turn, you may play that card, and you may spend mana as though it
 *   were mana of any color to cast it.
 *
 * A plain choose-one `modal(chooseCount = 1)`. The second mode is Mishra's Research Desk's
 * exile-then-choose-one shape over the targeted opponent's library (Siphon Insight's player
 * binding), with the turn-keyed [MayPlayExpiry.UntilEndOfNextTurn] grant on the chosen card only,
 * and `withAnyManaColor` for "as though it were mana of any color" (not the broader any-type).
 * The exile is face up; the unchosen cards stay exiled (ruling). "Play" includes lands, so the
 * grant isn't `nonLandOnly`.
 */
val YouFindSomePrisoners = card("You Find Some Prisoners") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Break Their Chains — Destroy target artifact.\n" +
        "• Interrogate Them — Exile the top three cards of target opponent's library. Choose one " +
        "of them. Until the end of your next turn, you may play that card, and you may spend mana " +
        "as though it were mana of any color to cast it."

    spell {
        modal(chooseCount = 1) {
            mode("Break Their Chains — Destroy target artifact.") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
            mode(
                "Interrogate Them — Exile the top three cards of target opponent's library. " +
                    "Choose one of them. Until the end of your next turn, you may play that card, " +
                    "and you may spend mana as though it were mana of any color to cast it."
            ) {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    val exiled = gather(CardSource.TopOfLibrary(3, opponent.asPlayer))
                    exile(exiled, opponent.asPlayer)
                    val chosen = chooseExactly(
                        1,
                        from = exiled,
                        prompt = "Choose a card you may play until the end of your next turn"
                    )
                    run(Effects.GrantMayPlayFromExile(
                        from = chosen,
                        expiry = MayPlayExpiry.UntilEndOfNextTurn,
                        withAnyManaColor = true,
                    ))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f2a95ae-feb2-42c3-99b4-9ea7a491d326.jpg?1783926469"
        ruling("2021-07-23", "For the Interrogate Them mode, the cards you didn't choose remain in exile. If you don't play the chosen card, it also remains in exile.")
    }
}
