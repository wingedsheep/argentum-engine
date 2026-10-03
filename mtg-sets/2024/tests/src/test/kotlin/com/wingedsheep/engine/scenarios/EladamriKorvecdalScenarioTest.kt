package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.EladamriKorvecdal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Eladamri, Korvecdal — {1}{G}{G} Legendary Creature — Elf Warrior 3/3
 *
 * You may look at the top card of your library any time.
 * You may cast creature spells from the top of your library.
 * {G}, {T}, Tap two untapped creatures you control: Reveal a card from your hand or the top card
 * of your library. If you reveal a creature card this way, put it onto the battlefield. Activate
 * only during your turn.
 */
class EladamriKorvecdalScenarioTest : FunSpec({

    val abilityId = EladamriKorvecdal.activatedAbilities.first().id

    val bigBeast = CardDefinition.creature(
        name = "Test Big Beast",
        manaCost = ManaCost.parse("{5}{G}{G}"),
        subtypes = emptySet(),
        power = 7,
        toughness = 7,
    )

    val greenBear = CardDefinition.creature(
        name = "Test Green Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EladamriKorvecdal, bigBeast, greenBear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    data class Board(val you: EntityId, val eladamri: EntityId, val helperA: EntityId, val helperB: EntityId)

    fun setUpBoard(driver: GameTestDriver): Board {
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val eladamri = driver.putCreatureOnBattlefield(you, "Eladamri, Korvecdal")
        driver.removeSummoningSickness(eladamri)
        val a = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val b = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 1)
        return Board(you, eladamri, a, b)
    }

    fun activate(driver: GameTestDriver, board: Board) = driver.submit(
        ActivateAbility(
            playerId = board.you,
            sourceId = board.eladamri,
            abilityId = abilityId,
            costPayment = AdditionalCostPayment(tappedPermanents = listOf(board.helperA, board.helperB))
        )
    )

    test("revealing a creature card from hand puts it onto the battlefield") {
        val driver = createDriver()
        val board = setUpBoard(driver)
        val beast = driver.putCardInHand(board.you, "Test Big Beast")

        activate(driver, board).error shouldBe null
        driver.isTapped(board.eladamri) shouldBe true
        driver.isTapped(board.helperA) shouldBe true
        driver.isTapped(board.helperB) shouldBe true

        driver.bothPass()
        driver.pendingDecision shouldNotBe null
        driver.submitCardSelection(board.you, listOf(beast))

        driver.getPermanents(board.you).contains(beast) shouldBe true
        driver.getHand(board.you).contains(beast) shouldBe false
    }

    test("choosing no hand card reveals the top card of the library and puts a creature onto the battlefield") {
        val driver = createDriver()
        val board = setUpBoard(driver)
        driver.putCardInHand(board.you, "Test Big Beast")
        val topBear = driver.putCardOnTopOfLibrary(board.you, "Test Green Bear")

        activate(driver, board).error shouldBe null
        driver.bothPass()
        driver.pendingDecision shouldNotBe null
        driver.submitCardSelection(board.you, emptyList())

        driver.getPermanents(board.you).contains(topBear) shouldBe true
    }

    test("a revealed noncreature card stays where it was") {
        val driver = createDriver()
        val board = setUpBoard(driver)
        val forest = driver.putCardInHand(board.you, "Forest")

        activate(driver, board).error shouldBe null
        driver.bothPass()
        driver.pendingDecision shouldNotBe null
        driver.submitCardSelection(board.you, listOf(forest))

        driver.getHand(board.you).contains(forest) shouldBe true
        driver.getPermanents(board.you).contains(forest) shouldBe false
    }

    test("with an empty hand the top card is revealed; a noncreature top card stays on top") {
        val driver = createDriver()
        val board = setUpBoard(driver)
        driver.getHand(board.you).forEach { driver.moveToGraveyard(it) }
        val topForest = driver.putCardOnTopOfLibrary(board.you, "Forest")

        activate(driver, board).error shouldBe null
        driver.bothPass()

        driver.getPermanents(board.you).contains(topForest) shouldBe false
        driver.state.getZone(board.you, com.wingedsheep.sdk.core.Zone.LIBRARY)
            .first() shouldBe topForest
    }

    test("creature spells can be cast from the top of the library") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(you, "Eladamri, Korvecdal")
        val bear = driver.putCardOnTopOfLibrary(you, "Test Green Bear")
        driver.giveMana(you, Color.GREEN, 2)

        driver.legalActions(you).any {
            it.actionType == "CastSpell" && it.description.contains("Test Green Bear") && it.sourceZone == "LIBRARY"
        } shouldBe true

        driver.castSpell(you, bear).error shouldBe null
        driver.bothPass()
        driver.getPermanents(you).contains(bear) shouldBe true
    }

    test("cannot be activated during an opponent's turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe opponent

        val eladamri = driver.putCreatureOnBattlefield(you, "Eladamri, Korvecdal")
        driver.removeSummoningSickness(eladamri)
        val a = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val b = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.putCardInHand(you, "Test Big Beast")
        driver.giveMana(you, Color.GREEN, 1)

        driver.passPriority(opponent)
        driver.state.priorityPlayerId shouldBe you

        activate(driver, Board(you, eladamri, a, b)).error shouldNotBe null
    }
})
