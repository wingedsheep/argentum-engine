package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A tracked move into a library whose order the player chooses pauses for that order. Once the
 * player answers, the moved cards are still published under the move's name, exactly as when the
 * move never paused (one card going to the bottom has no order to choose) — so "put them on the
 * bottom; if you did, …" reads the same either way. Lim-Dûl's Vault's repeat check is the card
 * that depends on it.
 */
class TrackedLibraryOrderMoveTest : FunSpec({

    /** Put the top [count] cards on the bottom in any order; if any went, draw a card. */
    fun bottomThenDraw(count: Int) = card("Bottom Then Draw $count") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(count))
                val bottomed = moveTracked(
                    top,
                    CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.ControllerChooses
                )
                ifNotEmpty(bottomed) { run(Effects.DrawCards(1)) }
            }
        }
    }

    class Cast(val d: GameTestDriver, val you: EntityId, val handAfterCast: Int)

    fun castAndResolve(count: Int): Cast {
        val spell = bottomThenDraw(count)
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(spell))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.player1
        val card = d.putCardInHand(you, spell.name)
        d.giveMana(you, Color.BLUE, 1)
        d.castSpell(you, card).error shouldBe null
        val handAfterCast = d.getHand(you).size
        d.bothPass()
        return Cast(d, you, handAfterCast)
    }

    test("an unpaused tracked move (one card to the bottom) publishes what it moved") {
        val c = castAndResolve(1)
        c.d.pendingDecision shouldBe null
        c.d.getHand(c.you).size shouldBe c.handAfterCast + 1
    }

    test("a tracked move that paused for the library order still publishes what it moved") {
        val c = castAndResolve(2)
        val order = c.d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        c.d.submitOrderedResponse(c.you, order.cards).error shouldBe null
        c.d.getHand(c.you).size shouldBe c.handAfterCast + 1
        c.d.state.getLibrary(c.you).takeLast(2) shouldBe order.cards
    }
})
