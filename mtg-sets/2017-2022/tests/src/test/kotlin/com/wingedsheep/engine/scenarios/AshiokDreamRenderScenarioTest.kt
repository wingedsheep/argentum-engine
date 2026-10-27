package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.AshiokDreamRender
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ashiok, Dream Render (WAR #228; {1}{U/B}{U/B}, Loyalty 5).
 *
 *   Spells and abilities your opponents control can't cause their controller to search their library.
 *   −1: Target player mills four cards. Then exile each opponent's graveyard.
 *
 * The search prohibition's rules matrix lives in the engine's `PlayersCantSearchLibrariesTest`;
 * this pins the card.
 */
class AshiokDreamRenderScenarioTest : FunSpec({
    val tutor = card("Probe Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibrary(GameObjectFilter.Any) } }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AshiokDreamRender, tutor))
        initMirrorMatch(Deck.of("Island" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    // Put onto the battlefield directly, Ashiok has no loyalty counters until given its five.
    fun GameTestDriver.putAshiok(controller: EntityId): EntityId {
        val ashiok = putPermanentOnBattlefield(controller, "Ashiok, Dream Render")
        replaceState(state.updateEntity(ashiok) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5)) })
        return ashiok
    }

    test("an opponent's own search of their library finds nothing but still shuffles") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putAshiok(opp)
        val handBefore = d.getHandSize(me)
        d.castSpell(me, d.putCardInHand(me, "Probe Tutor")).error shouldBe null
        val events = d.bothPass().events
        withClue("no search choice — Ashiok's controller is my opponent") { d.state.pendingDecision shouldBe null }
        d.getHandSize(me) shouldBe handBefore
        events.any { it is LibraryShuffledEvent } shouldBe true
    }

    test("Ashiok's controller still searches") {
        val d = driver(); val me = d.activePlayer!!
        d.putAshiok(me)
        d.castSpell(me, d.putCardInHand(me, "Probe Tutor")).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("−1 mills the target opponent four and exiles every opponent's graveyard, the milled cards included") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val ashiok = d.putAshiok(me)
        d.putCardInGraveyard(opp, "Island")
        d.putCardInGraveyard(me, "Island")
        val oppLibrary = d.state.getLibrary(opp).size
        val exileBefore = d.getExile(opp).size

        d.submit(
            ActivateAbility(me, ashiok, AshiokDreamRender.activatedAbilities[0].id, targets = listOf(ChosenTarget.Player(opp)))
        ).error shouldBe null
        d.bothPass()

        d.state.getLibrary(opp).size shouldBe oppLibrary - 4
        withClue("the opponent's graveyard, four milled cards and the earlier Island, is exiled") {
            d.getGraveyard(opp).size shouldBe 0
            d.getExile(opp).size shouldBe exileBefore + 5
        }
        withClue("Ashiok's controller keeps their graveyard") { d.getGraveyard(me).size shouldBe 1 }
        d.state.getEntity(ashiok)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 4
    }
})
