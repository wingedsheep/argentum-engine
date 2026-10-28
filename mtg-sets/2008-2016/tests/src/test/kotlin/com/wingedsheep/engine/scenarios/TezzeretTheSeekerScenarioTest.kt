package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ala.cards.TezzeretTheSeeker
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tezzeret the Seeker — "−X: Search your library for an artifact card with mana value X or less,
 * put it onto the battlefield, then shuffle."
 *
 * The X in the search filter is the one chosen as the loyalty cost is paid, not a spell's {X}:
 * this proves `manaValueAtMostX()` reads the loyalty ability's locked-in X at resolution.
 */
class TezzeretTheSeekerScenarioTest : FunSpec({
    val twoDrop = card("Tezzeret Test Two-Drop Artifact") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    val threeDrop = card("Tezzeret Test Three-Drop Artifact") {
        manaCost = "{3}"
        typeLine = "Artifact"
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(TezzeretTheSeeker, twoDrop, threeDrop))
        it.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun putTezzeret(d: GameTestDriver, player: EntityId): EntityId =
        d.putPermanentOnBattlefield(player, "Tezzeret the Seeker").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 4)))
        }

    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    val minusX = TezzeretTheSeeker.script.activatedAbilities[1]

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("minus X finds an artifact with mana value X or less") {
        val d = driver()
        val me = d.player1
        val tez = putTezzeret(d, me)
        val artifact = d.putCardOnTopOfLibrary(me, "Tezzeret Test Two-Drop Artifact")

        d.submitSuccess(ActivateAbility(me, tez, minusX.id, xValue = 2))
        loyalty(d, tez) shouldBe 2

        var guard = 0
        while (!d.isPaused && d.state.stack.isNotEmpty() && guard++ < 10) d.bothPass()
        d.submitCardSelection(me, listOf(artifact))
        d.settle()

        withClue("the mana value 2 artifact is within X = 2 and enters the battlefield") {
            d.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(artifact) shouldBe true
        }
    }

    test("minus X can't find an artifact above X") {
        val d = driver()
        val me = d.player1
        val tez = putTezzeret(d, me)
        val artifact = d.putCardOnTopOfLibrary(me, "Tezzeret Test Three-Drop Artifact")

        d.submitSuccess(ActivateAbility(me, tez, minusX.id, xValue = 2))

        var guard = 0
        while (!d.isPaused && d.state.stack.isNotEmpty() && guard++ < 10) d.bothPass()
        if (d.isPaused) {
            withClue("the search must not offer the mana value 3 artifact at X = 2") {
                ((d.pendingDecision as? SelectCardsDecision)?.options ?: emptyList()).contains(artifact) shouldBe false
            }
            d.submitCardSelection(me, emptyList())
        }
        d.settle()

        withClue("the mana value 3 artifact stays in the library") {
            d.state.getZone(ZoneKey(me, Zone.LIBRARY)).contains(artifact) shouldBe true
        }
    }
})
