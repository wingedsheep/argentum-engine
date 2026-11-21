package com.wingedsheep.engine

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * [RevealCollectionEffect.turnFaceUp] — "turn face up all cards exiled with ~" (Pyxis of
 * Pandemonium). Pinned here: a face-down exiled card is turned face up where it is — same entity
 * id, same exile zone, no face-down markers left — and the opponent's log names it; a face-down
 * permanent in the same collection is not touched (turning a permanent up is the morph/manifest
 * special action, not this step). A plain face-down exile hides the card from its owner as well
 * (CR 708.5 gives exile no controller baseline). Card coverage lives in `PyxisOfPandemoniumScenarioTest`.
 */
class TurnFaceUpInExileTest : FunSpec({

    val ExileThenTurnUp = card("Exile Then Turn Up Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(2)))
                exile(top, faceDown = FaceDownMode.HIDDEN)
                turnFaceUp(top)
            }
        }
    }

    val ExileFaceDown = card("Exile Face Down Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), lookAudience = LookAudience.None)
                exile(top, faceDown = FaceDownMode.HIDDEN)
            }
        }
    }

    // Hideaway's shape: look at the top card, then exile it face down.
    val LookThenExileFaceDown = card("Look Then Exile Face Down Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                exile(top, faceDown = FaceDownMode.HIDDEN)
            }
        }
    }

    val ManifestThenTurnUp = card("Manifest Then Turn Up Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                move(top, CardDestination.ToZone(Zone.BATTLEFIELD), faceDown = FaceDownMode.MANIFEST)
                turnFaceUp(top)
            }
        }
    }

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ExileThenTurnUp, ExileFaceDown, LookThenExileFaceDown, ManifestThenTurnUp))
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String) {
        val spell = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass()
    }

    test("face-down exiled cards are turned face up in place and named to the opponent") {
        val d = newDriver()
        val second = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val first = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")

        d.cast("Exile Then Turn Up Test")

        d.state.getZone(ZoneKey(d.player1, Zone.EXILE)) shouldContainExactly listOf(first, second)
        for (id in listOf(first, second)) {
            val entity = d.state.getEntity(id)!!
            entity.has<FaceDownComponent>() shouldBe false
            entity.has<FaceDownModeComponent>() shouldBe false
        }
        val opponentLog = ClientEventTransformer.transform(d.events, d.player2, d.state)
            .joinToString("\n") { it.description }
        opponentLog shouldContain "Grizzly Bears"
        opponentLog shouldContain "Centaur Courser"
    }

    test("a plain face-down exile hides the card from its owner too, until it is turned face up") {
        val d = newDriver()
        // An unknown library card (putCardOnTopOfLibrary would mark it known to its owner),
        // exiled without anyone looking at it.
        val hidden = d.state.getLibrary(d.player1).first()
        d.cast("Exile Face Down Test")
        val view = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        for (viewer in listOf(d.player1, d.player2)) {
            val seen = view.transform(d.state, viewingPlayerId = viewer).cards[hidden]!!
            seen.isFaceDown shouldBe true
            (seen.name == "Island") shouldBe false
            seen.revealedName shouldBe null
        }

        val top = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        d.cast("Exile Then Turn Up Test")
        for (viewer in listOf(d.player1, d.player2)) {
            view.transform(d.state, viewingPlayerId = viewer).cards[top]!!.name shouldBe "Centaur Courser"
        }
    }

    test("a player who looked at a card before exiling it face down may keep looking (CR 406.3)") {
        val d = newDriver()
        val looked = d.state.getLibrary(d.player1).first()
        d.cast("Look Then Exile Face Down Test")
        val view = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        view.transform(d.state, viewingPlayerId = d.player1).cards[looked]!!.name shouldBe "Island"
        (view.transform(d.state, viewingPlayerId = d.player2).cards[looked]!!.name == "Island") shouldBe false
    }

    test("a face-down permanent in the collection stays face down") {
        val d = newDriver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")

        d.cast("Manifest Then Turn Up Test")

        d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD)).contains(top) shouldBe true
        d.state.getEntity(top)!!.has<FaceDownComponent>() shouldBe true
        d.state.getEntity(top)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
    }
})
