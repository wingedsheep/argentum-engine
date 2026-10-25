package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MayLookAtInExileComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.RogueClass
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rogue Class {U}{B} — Enchantment — Class (AFR #230).
 *
 * Pins the linked exile end to end: level 1's combat-damage trigger exiles the *damaged player's*
 * top card face down, links it to this Class and lets only its controller look; level 3's static
 * makes exactly that pile playable with mana of any color (and nothing else in exile); level 2's
 * menace anthem is gated on the reached level.
 */
class RogueClassScenarioTest : FunSpec({

    // {G}{G}: paying it from Islands proves "as though it were mana of any color".
    val greenBear = card("Rogue Test Bear") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    data class Board(val driver: GameTestDriver, val you: EntityId, val opponent: EntityId, val rogue: EntityId)

    fun setup(level: Int): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RogueClass)
        driver.registerCard(greenBear)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val rogue = driver.putPermanentOnBattlefield(you, "Rogue Class", classLevel = level)
        return Board(driver, you, driver.getOpponent(you), rogue)
    }

    /** Attack with a fresh Grizzly Bears, no blocks, and land in the postcombat main phase. */
    fun Board.connect() {
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bears), opponent).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    test("level 1: combat damage exiles that player's top card face down, linked, and only you may look") {
        val board = setup(level = 1)
        val (driver, you, opponent, rogue) = board
        val stolen = driver.putCardOnTopOfLibrary(opponent, "Rogue Test Bear")

        board.connect()

        withClue("the damaged player's top card is in exile") {
            driver.state.getZone(opponent, Zone.EXILE).contains(stolen) shouldBe true
        }
        withClue("it is exiled face down") {
            driver.state.getEntity(stolen)!!.has<FaceDownComponent>() shouldBe true
        }
        withClue("it is linked to this Rogue Class") {
            driver.state.getEntity(rogue)?.get<LinkedExileComponent>()?.exiledIds?.contains(stolen) shouldBe true
        }
        val look = driver.state.getEntity(stolen)?.get<MayLookAtInExileComponent>()
        withClue("its controller may look at it; the opponent may not") {
            look?.mayLook(you) shouldBe true
            look?.mayLook(opponent) shouldBe false
        }
        withClue("level 1 grants no permission to play it") {
            repeat(2) { driver.putLandOnBattlefield(you, "Island") }
            driver.canCast(you, stolen) shouldBe false
        }
    }

    test("level 3: a card exiled with the Class is castable with mana of any color; other exiled cards are not") {
        val board = setup(level = 3)
        val (driver, you, opponent, _) = board
        val stolen = driver.putCardOnTopOfLibrary(opponent, "Rogue Test Bear")
        val unlinked = driver.putCardInExile(opponent, "Rogue Test Bear")

        board.connect()
        repeat(2) { driver.putLandOnBattlefield(you, "Island") }

        withClue("a card in exile the Class didn't exile is not playable") {
            driver.canCast(you, unlinked) shouldBe false
        }
        withClue("{G}{G} is payable from Islands") {
            driver.canCast(you, stolen) shouldBe true
        }
        driver.castSpell(you, stolen).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getZone(you, Zone.BATTLEFIELD).contains(stolen) shouldBe true
    }

    test("level 2 gives creatures you control menace; level 1 doesn't") {
        val low = setup(level = 1)
        val lowBears = low.driver.putCreatureOnBattlefield(low.you, "Grizzly Bears")
        low.driver.state.projectedState.hasKeyword(lowBears, Keyword.MENACE) shouldBe false

        val high = setup(level = 2)
        val highBears = high.driver.putCreatureOnBattlefield(high.you, "Grizzly Bears")
        val theirBears = high.driver.putCreatureOnBattlefield(high.opponent, "Grizzly Bears")
        high.driver.state.projectedState.hasKeyword(highBears, Keyword.MENACE) shouldBe true
        high.driver.state.projectedState.hasKeyword(theirBears, Keyword.MENACE) shouldBe false
    }
})
