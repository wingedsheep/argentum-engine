package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoc.cards.EvendoBrushrazer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Evendo Brushrazer (EOC #10) — {2}{R} 2/2 Creature — Insect Warrior.
 *
 * "Whenever you sacrifice a nontoken permanent, exile the top card of your library.
 *  During your turn, as long as you've sacrificed a nontoken permanent this turn, you may play
 *  cards exiled with this creature.
 *  {T}, Sacrifice a land: Add {R}{R}."
 *
 * Pins the nontoken sacrifice trigger, the linked exile, and the play permission gated on the
 * per-turn `NONTOKEN_PERMANENTS_SACRIFICED` record: a token sacrifice neither exiles nor unlocks,
 * and the permission lapses with the record at the turn boundary.
 */
class EvendoBrushrazerScenarioTest : FunSpec({

    val shock = card("Evendo Test Spark") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val makeToken = card("Evendo Test Token Maker") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Create a 1/1 Soldier creature token."
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier")) }
    }
    val altar = card("Evendo Test Altar") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "Sacrifice a creature: You gain 1 life."
        activatedAbility {
            cost = Costs.Sacrifice(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }

    val manaAbilityId = EvendoBrushrazer.activatedAbilities.first().id

    data class Board(val driver: GameTestDriver, val you: EntityId, val evendo: EntityId)

    fun setup(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EvendoBrushrazer, shock, makeToken, altar))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val evendo = driver.putCreatureOnBattlefield(you, "Evendo Brushrazer")
        driver.removeSummoningSickness(evendo)
        return Board(driver, you, evendo)
    }

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    fun Board.sacrificeLandForMana() {
        val land = driver.putLandOnBattlefield(you, "Mountain")
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = evendo,
                abilityId = manaAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(land)),
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("sacrificing a land exiles the top card, linked, and it is playable this turn") {
        val board = setup()
        val (driver, you, evendo) = board
        val spark = driver.putCardOnTopOfLibrary(you, "Evendo Test Spark")

        board.sacrificeLandForMana()

        withClue("the top card is exiled and linked to Evendo") {
            driver.state.getZone(you, Zone.EXILE).contains(spark) shouldBe true
            driver.state.getEntity(evendo)?.get<LinkedExileComponent>()?.exiledIds?.contains(spark) shouldBe true
        }
        driver.putLandOnBattlefield(you, "Mountain")
        withClue("you've sacrificed a nontoken permanent this turn, so it's playable") {
            driver.canCast(you, spark) shouldBe true
        }
        driver.castSpell(you, spark).outcome shouldBe Outcome.Done
    }

    test("a token sacrifice neither exiles a card nor unlocks the exiled ones; the permission ends with the turn") {
        val board = setup()
        val (driver, you, _) = board
        val spark = driver.putCardOnTopOfLibrary(you, "Evendo Test Spark")
        board.sacrificeLandForMana()

        // Move on to your next turn: the nontoken-sacrifice record has reset.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = driver.getOpponent(you))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = you)
        driver.putLandOnBattlefield(you, "Mountain")
        withClue("a new turn without a nontoken sacrifice — not playable") {
            driver.canCast(you, spark) shouldBe false
        }

        driver.putPermanentOnBattlefield(you, "Evendo Test Altar")
        val maker = driver.putCardInHand(you, "Evendo Test Token Maker")
        driver.castSpell(you, maker).outcome shouldBe Outcome.Done
        driver.bothPass()
        val token = driver.getPermanents(you).single { driver.state.getEntity(it)?.has<TokenComponent>() == true }
        val exileBefore = driver.state.getZone(you, Zone.EXILE).size
        val altarId = driver.findPermanent(you, "Evendo Test Altar")!!
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = altarId,
                abilityId = altar.activatedAbilities.first().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(token)),
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        withClue("sacrificing a token doesn't trigger Evendo") {
            driver.state.getZone(you, Zone.EXILE).size shouldBe exileBefore
        }
        withClue("nor does it satisfy the nontoken-sacrifice condition") {
            driver.canCast(you, spark) shouldBe false
        }
    }
})
