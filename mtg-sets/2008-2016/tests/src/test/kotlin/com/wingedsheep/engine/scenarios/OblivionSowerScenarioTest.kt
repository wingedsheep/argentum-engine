package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ddp.cards.OblivionSower
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Oblivion Sower (DDP #41, BFZ #11): "When you cast this spell, target opponent exiles the top
 * four cards of their library, then you may put any number of land cards that player owns from
 * exile onto the battlefield under your control."
 *
 * Pins down the composition: the pick spans every land card the targeted opponent owns in exile
 * (not only the four just exiled), never a card you own or a nonland, and the chosen lands enter
 * under your control while the Sower is still on the stack.
 */
class OblivionSowerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OblivionSower))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(6) { driver.putLandOnBattlefield(you, "Swamp") }
        return driver to you
    }

    /**
     * Casts Oblivion Sower targeting [opponent] and resolves only its cast trigger, answering the
     * land pick with [pick]. Returns the options the pick offered, or null when none was asked.
     */
    fun GameTestDriver.castAndResolveTrigger(you: EntityId, opponent: EntityId, pick: (List<EntityId>) -> List<EntityId>): List<EntityId>? {
        castSpell(you, putCardInHand(you, "Oblivion Sower")).error shouldBe null
        var offered: List<EntityId>? = null
        var safety = 0
        while ((state.stack.size > 1 || pendingDecision != null) && safety++ < 20) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is ChooseTargetsDecision -> submitTargetSelection(you, listOf(opponent)).error shouldBe null
                is SelectCardsDecision -> {
                    offered = decision.options
                    submitCardSelection(you, pick(decision.options)).error shouldBe null
                }
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        // The creature spell is still waiting on the stack.
        state.stack.size shouldBe 1
        return offered
    }

    test("any land the opponent owns in exile can be put onto the battlefield under your control") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        // Library top, top-first: Forest, Centaur Courser, Island, Savannah Lions.
        val lions = driver.putCardOnTopOfLibrary(opponent, "Savannah Lions")
        val island = driver.putCardOnTopOfLibrary(opponent, "Island")
        val courser = driver.putCardOnTopOfLibrary(opponent, "Centaur Courser")
        val forest = driver.putCardOnTopOfLibrary(opponent, "Forest")
        val alreadyExiled = driver.putCardInExile(opponent, "Mountain")
        val yours = driver.putCardInExile(you, "Plains")

        val offered = driver.castAndResolveTrigger(you, opponent) { listOf(forest, alreadyExiled) }

        offered!! shouldContainExactlyInAnyOrder listOf(forest, island, alreadyExiled)
        offered shouldNotContain yours

        driver.state.getBattlefield() shouldContainAll listOf(forest, alreadyExiled)
        driver.getController(forest) shouldBe you
        driver.getController(alreadyExiled) shouldBe you
        driver.state.getZone(opponent, Zone.EXILE) shouldContainAll listOf(island, courser, lions)
        driver.state.getZone(you, Zone.EXILE) shouldContain yours
    }

    test("declining leaves the four exiled cards in exile") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val top = (1..4).map { driver.putCardOnTopOfLibrary(opponent, "Forest") }
        val battlefieldBefore = driver.state.getBattlefield().size

        val offered = driver.castAndResolveTrigger(you, opponent) { emptyList() }

        offered!! shouldContainExactlyInAnyOrder top
        driver.state.getZone(opponent, Zone.EXILE) shouldContainAll top
        driver.state.getBattlefield().size shouldBe battlefieldBefore
    }
})
