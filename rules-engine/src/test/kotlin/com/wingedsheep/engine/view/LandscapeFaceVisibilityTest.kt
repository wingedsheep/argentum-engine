package com.wingedsheep.engine.view

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.PainSuffering
import com.wingedsheep.mtg.sets.definitions.mom.cards.InvasionOfInnistrad
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A battle (CR 310) is printed **landscape**: its Scryfall image is an ordinary portrait file
 * containing a card lying on its side, exactly like a Room's. Nothing else about a battle tells the
 * client that — it's a transforming double-faced card, so its layout is `TRANSFORM` and it carries
 * a `backFace` rather than the `cardFaces` the split-card renderer keys on — so
 * [ClientStateTransformer] surfaces it as [ClientCard.isLandscapeFace]. Without the flag a battle
 * renders sideways and unreadable on the battlefield and in every hover preview.
 *
 * The flag is per **face**, not per card: Invasion of Innistrad is landscape, and the Deluge of the
 * Dead face it becomes once defeated is an ordinary portrait enchantment.
 */
class LandscapeFaceVisibilityTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(InvasionOfInnistrad, PainSuffering))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    test("a battle in hand is flagged isLandscapeFace so the preview rotates it") {
        val d = driver()
        val player = d.activePlayer!!
        val card = d.putCardInHand(player, "Invasion of Innistrad")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[card]?.isLandscapeFace shouldBe true
    }

    test("a battle on the battlefield is flagged isLandscapeFace") {
        val d = driver()
        val player = d.activePlayer!!
        val battle = d.putPermanentOnBattlefield(player, "Invasion of Innistrad")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[battle]?.isLandscapeFace shouldBe true
    }

    test("an ordinary portrait permanent is not flagged") {
        val d = driver()
        val player = d.activePlayer!!
        val bears = d.putPermanentOnBattlefield(player, "Grizzly Bears")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[bears]?.isLandscapeFace shouldBe false
    }

    test("a split-layout card reports the same flag — one predicate covers both families") {
        // The point of generalizing: split cards were rotated by a *separate* client-side check
        // (`isRoom` / `cardFaces.length === 2` / `layout === 'SPLIT'`), which is exactly why
        // battles fell through. Both now travel on the one flag.
        val d = driver()
        val player = d.activePlayer!!
        val card = d.putCardInHand(player, "Pain // Suffering")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[card]?.isLandscapeFace shouldBe true
    }

    test("a battle's portrait back face is reported separately, for the flip preview") {
        val d = driver()
        val player = d.activePlayer!!
        val battle = d.putPermanentOnBattlefield(player, "Invasion of Innistrad")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        withClue("flipping the preview shows Deluge of the Dead, which is an ordinary enchantment") {
            view.cards[battle]?.backFaceIsLandscape shouldBe false
        }
    }
})
