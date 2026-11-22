package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m11.cards.ElixirOfImmortality
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Elixir of Immortality {1} — Artifact.
 *
 * "{2}, {T}: You gain 5 life. Shuffle this artifact and your graveyard into their owner's library."
 *
 * The script gathers the Elixir (still on the battlefield) and the graveyard into one collection and
 * shuffles it in with a single move. Pins: the life gain, the Elixir and every graveyard card ending
 * up in the library, the library being shuffled exactly once (not once per half), and the ruling
 * that an Elixir already in your graveyard at resolution is shuffled in as part of the graveyard.
 */
class ElixirOfImmortalityScenarioTest : FunSpec({

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + ElixirOfImmortality)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingLife = 20)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val elixir = d.putPermanentOnBattlefield(me, "Elixir of Immortality")
        return Triple(d, me, elixir)
    }

    fun resolveAll(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()
    }

    val abilityId = ElixirOfImmortality.activatedAbilities.first().id

    test("gains 5 life and shuffles the Elixir and the graveyard into the library in one shuffle") {
        val (d, me, elixir) = setUp()
        val bears = d.putCardInGraveyard(me, "Centaur Courser")
        val bolt = d.putCardInGraveyard(me, "Lightning Bolt")
        val librarySize = d.state.getZone(me, Zone.LIBRARY).size
        val eventsBefore = d.events.size

        d.giveColorlessMana(me, 2)
        d.submitSuccess(ActivateAbility(me, elixir, abilityId))
        resolveAll(d)

        d.getLifeTotal(me) shouldBe 25
        d.getGraveyard(me).isEmpty() shouldBe true
        d.findPermanent(me, "Elixir of Immortality") shouldBe null
        val library = d.state.getZone(me, Zone.LIBRARY)
        library.size shouldBe librarySize + 3
        withClue("the Elixir and both graveyard cards are in the library") {
            listOf(elixir, bears, bolt).all { it in library } shouldBe true
        }
        withClue("one shuffle of the library, not one per half") {
            d.events.drop(eventsBefore).filterIsInstance<LibraryShuffledEvent>()
                .count { it.playerId == me } shouldBe 1
        }
    }

    test("with an empty graveyard the Elixir alone is shuffled in") {
        val (d, me, elixir) = setUp()
        val librarySize = d.state.getZone(me, Zone.LIBRARY).size

        d.giveColorlessMana(me, 2)
        d.submitSuccess(ActivateAbility(me, elixir, abilityId))
        resolveAll(d)

        d.getLifeTotal(me) shouldBe 25
        d.state.getZone(me, Zone.LIBRARY).size shouldBe librarySize + 1
        (elixir in d.state.getZone(me, Zone.LIBRARY)) shouldBe true
    }

    test("an Elixir that went to your graveyard in response is shuffled in with the graveyard") {
        val (d, me, elixir) = setUp()
        val bears = d.putCardInGraveyard(me, "Centaur Courser")
        val librarySize = d.state.getZone(me, Zone.LIBRARY).size

        d.giveColorlessMana(me, 2)
        d.submitSuccess(ActivateAbility(me, elixir, abilityId))
        d.moveToGraveyard(elixir)
        resolveAll(d)

        d.getLifeTotal(me) shouldBe 25
        d.getGraveyard(me).isEmpty() shouldBe true
        d.state.getZone(me, Zone.LIBRARY).size shouldBe librarySize + 2
        withClue("both the graveyard Elixir and the Bears are in the library") {
            (elixir in d.state.getZone(me, Zone.LIBRARY) && bears in d.state.getZone(me, Zone.LIBRARY)) shouldBe true
        }
    }
})
