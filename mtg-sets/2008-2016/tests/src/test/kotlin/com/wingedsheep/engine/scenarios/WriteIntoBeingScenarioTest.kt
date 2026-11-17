package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.WriteIntoBeing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Write into Being (FRF) — {2}{U} Sorcery. "Look at the top two cards of your library. Manifest one of
 * those cards, then put the other on the top or bottom of your library."
 */
class WriteIntoBeingScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + WriteIntoBeing)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
    }

    /** Stack [second] then [top] on the library, cast Write into Being, resolve to the manifest pick. */
    fun GameTestDriver.castOver(you: EntityId, second: String, top: String): Pair<EntityId, EntityId> {
        val secondId = putCardOnTopOfLibrary(you, second)
        val topId = putCardOnTopOfLibrary(you, top)
        val spell = putCardInHand(you, "Write into Being")
        giveMana(you, Color.BLUE, 3)
        castSpell(you, spell).error shouldBe null
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
        return topId to secondId
    }

    fun GameTestDriver.pick(you: EntityId, cards: List<EntityId>) {
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitDecision(you, CardsSelectedResponse(decisionId = decision.id, selectedCards = cards))
    }

    test("manifests the chosen second card as a face-down 2/2 and puts the other on the bottom") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (top, second) = d.castOver(you, second = "Centaur Courser", top = "Forest")

        val manifestPick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        manifestPick.options.toSet() shouldBe setOf(top, second)
        d.pick(you, listOf(second))

        d.getPermanents(you) shouldContain second
        val entity = d.state.getEntity(second)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        d.state.projectedState.isCreature(second) shouldBe true
        d.state.projectedState.getPower(second) shouldBe 2
        d.state.projectedState.getToughness(second) shouldBe 2

        // Then the other card: choosing it sends it to the bottom.
        val placePick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        placePick.options shouldBe listOf(top)
        d.pick(you, listOf(top))

        val library = d.state.getLibrary(you)
        library.last() shouldBe top
        library.dropLast(1) shouldNotContain top
        d.state.stack.isEmpty() shouldBe true
    }

    test("declining the bottom placement leaves the other card on top") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (top, second) = d.castOver(you, second = "Forest", top = "Centaur Courser")

        d.pick(you, listOf(top))
        d.state.getEntity(top)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST

        d.pick(you, emptyList())

        d.state.getLibrary(you).first() shouldBe second
        d.getPermanents(you) shouldNotContain second
    }
})
