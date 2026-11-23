package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.NightveilSpecter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nightveil Specter {U/B}{U/B}{U/B} — Creature — Specter (GTC #222).
 *
 * Pins the linked exile: combat damage exiles the damaged player's top card face up and links it to
 * this Specter; its static lets you cast spells and play lands from exactly that pile (not other
 * exiled cards), and the permission ends once the Specter leaves the battlefield.
 */
class NightveilSpecterScenarioTest : FunSpec({

    val blueBear = card("Specter Test Drake") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Drake"
        power = 2
        toughness = 2
    }

    data class Board(val driver: GameTestDriver, val you: EntityId, val opponent: EntityId, val specter: EntityId)

    fun setup(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NightveilSpecter)
        driver.registerCard(blueBear)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val specter = driver.putCreatureOnBattlefield(you, "Nightveil Specter")
        driver.removeSummoningSickness(specter)
        return Board(driver, you, driver.getOpponent(you), specter)
    }

    /** Attack with the Specter, no blocks, and land in the postcombat main phase. */
    fun Board.connect() {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(specter), opponent).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    fun GameTestDriver.canPlayLand(playerId: EntityId, cardId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? PlayLand)?.cardId == cardId }

    test("combat damage exiles that player's top card face up and linked; you may cast it, not other exiled cards") {
        val board = setup()
        val (driver, you, opponent, specter) = board
        val stolen = driver.putCardOnTopOfLibrary(opponent, "Specter Test Drake")
        val unlinked = driver.putCardInExile(opponent, "Specter Test Drake")

        board.connect()

        withClue("the damaged player's top card is in exile, face up") {
            driver.state.getZone(opponent, Zone.EXILE).contains(stolen) shouldBe true
            driver.state.getEntity(stolen)!!.has<FaceDownComponent>() shouldBe false
        }
        withClue("it is linked to this Specter") {
            driver.state.getEntity(specter)?.get<LinkedExileComponent>()?.exiledIds?.contains(stolen) shouldBe true
        }
        repeat(2) { driver.putLandOnBattlefield(you, "Island") }
        withClue("a card in exile the Specter didn't exile is not castable") {
            driver.canCast(you, unlinked) shouldBe false
        }
        withClue("the opponent can't cast the card exiled with your Specter") {
            driver.canCast(opponent, stolen) shouldBe false
        }
        driver.canCast(you, stolen) shouldBe true
        driver.castSpell(you, stolen).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getZone(you, Zone.BATTLEFIELD).contains(stolen) shouldBe true
    }

    test("a land exiled with the Specter can be played as your land drop") {
        val board = setup()
        val (driver, you, opponent, _) = board
        val stolenLand = driver.putCardOnTopOfLibrary(opponent, "Island")

        board.connect()

        driver.state.getZone(opponent, Zone.EXILE).contains(stolenLand) shouldBe true
        driver.canPlayLand(you, stolenLand) shouldBe true
        driver.playLand(you, stolenLand).error shouldBe null
        driver.state.getZone(you, Zone.BATTLEFIELD).contains(stolenLand) shouldBe true
    }

    test("the permission ends when the Specter leaves the battlefield") {
        val board = setup()
        val (driver, you, opponent, specter) = board
        val stolen = driver.putCardOnTopOfLibrary(opponent, "Specter Test Drake")

        board.connect()
        repeat(2) { driver.putLandOnBattlefield(you, "Island") }
        driver.canCast(you, stolen) shouldBe true

        driver.moveToGraveyard(specter)

        withClue("the card stays exiled but is no longer castable") {
            driver.state.getZone(opponent, Zone.EXILE).contains(stolen) shouldBe true
            driver.canCast(you, stolen) shouldBe false
        }
    }
})
