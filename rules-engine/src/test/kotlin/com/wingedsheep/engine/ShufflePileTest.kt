package com.wingedsheep.engine

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.ShufflePileEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * [ShufflePileEffect] — "exile it and the top card of your library in a face-down pile, shuffle that
 * pile" (Jeskai Infiltrator). Pinned here: the collections are joined into one pile, each card once; a
 * face-down card outside the library is given a fresh id as the pile is shuffled, so nobody can follow
 * it through the shuffle (the Infiltrator's ruling: "The pile is shuffled to disguise from your
 * opponents which manifested creature is which"); a face-up card keeps its id; the re-identified card
 * keeps everything else — its zone, its exile link to the source; and the source collections are
 * rewritten so a later step reading one still finds its cards. Card coverage lives in
 * `JeskaiInfiltratorScenarioTest`.
 */
class ShufflePileTest : FunSpec({

    // Exile the top two cards face down, shuffle them together with the first again (a duplicate),
    // then put the "first" collection into the graveyard — it must still find its card under the
    // new id.
    val FaceDownPile = card("Face-Down Pile Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val first = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                exile(first, faceDown = FaceDownMode.HIDDEN)
                val second = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
                exile(second, faceDown = FaceDownMode.HIDDEN)
                shufflePile(listOf(first, second, first))
                toGraveyard(first)
            }
        }
    }

    // The same pile exiled face up: nothing is hidden, so nothing is renamed.
    val FaceUpPile = card("Face-Up Pile Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(2)))
                exile(top)
                val pile = shufflePile(listOf(top))
                move(pile, CardDestination.ToZone(Zone.GRAVEYARD))
            }
        }
    }

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(FaceDownPile, FaceUpPile))
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String): EntityId {
        val spell = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass()
        return spell
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    test("face-down cards in the pile get fresh ids; duplicates count once and the source collections follow") {
        val d = newDriver()
        val second = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val first = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")

        d.cast("Face-Down Pile Test")

        // The duplicate counted once: exactly the two cards were joined and renamed.
        d.state.getEntity(first) shouldBe null
        d.state.getEntity(second) shouldBe null
        // "first" was rewritten to the Bears' new id, so the follow-up move found it.
        val graveyard = d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))
        graveyard.map { d.name(it) } shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Face-Down Pile Test")
        graveyard shouldNotContainAnyOf listOf(first, second)
        // The Courser stays face down in exile under its new id, the same object as before.
        val exiled = d.state.getZone(ZoneKey(d.player1, Zone.EXILE)).single()
        d.name(exiled) shouldBe "Centaur Courser"
        d.state.getEntity(exiled)!!.has<FaceDownComponent>() shouldBe true
        d.state.logicalZone(exiled) shouldBe ZoneKey(d.player1, Zone.EXILE)
    }

    test("the log never names a card that stays hidden, though its events were emitted under its old id") {
        val d = newDriver()
        d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")

        d.cast("Face-Down Pile Test")

        // The Courser went library -> exile face down under an id the shuffle then retired.
        val opponentLog = ClientEventTransformer.transform(d.events, d.player2, d.state).joinToString("\n") { it.description }
        opponentLog shouldNotContain "Centaur Courser"
        // The Bears were put into the graveyard, revealed as they went: still named.
        opponentLog shouldContain "Grizzly Bears"
    }

    test("re-identifying a card renames it in its zone, in the exile pile it is linked to and in play grants") {
        val d = newDriver()
        val source = d.putPermanentOnBattlefield(d.player1, "Centaur Courser")
        val exiled = d.putCardInExile(d.player1, "Grizzly Bears")
        val before = d.state.updateEntity(source) { it.with(LinkedExileComponent(listOf(exiled))) }
        val generation = before.objectRef(exiled)!!.generation

        val (newId, after) = before.reidentify(exiled)

        (newId == exiled) shouldBe false
        after.getEntity(exiled) shouldBe null
        after.getEntity(newId)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        after.getZone(ZoneKey(d.player1, Zone.EXILE)) shouldBe listOf(newId)
        after.objectRef(newId)!!.generation shouldBe generation
        after.getEntity(source)!!.get<LinkedExileComponent>()!!.exiledIds shouldBe listOf(newId)
        // A permanent is never renamed: too much points at it.
        before.reidentify(source) shouldBe (source to before)
    }

    test("face-up cards keep their ids — anyone can tell them apart") {
        val d = newDriver()
        val a = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val b = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")

        d.cast("Face-Up Pile Test")

        val graveyard = d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))
        graveyard.filter { d.name(it) != "Face-Up Pile Test" } shouldContainExactlyInAnyOrder listOf(a, b)
    }
})
