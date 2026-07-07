package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.HulkingRaptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hulking Raptor (LCI #191) — {2}{G}{G} Creature — Dinosaur 5/3.
 *
 * "Ward {2}
 *  At the beginning of your first main phase, add {G}{G}."
 *
 * Covered:
 *  1. First-main trigger: when HulkingRaptor is on the battlefield under the active player's
 *     control, the precombat main phase fires [Triggers.FirstMainPhase], adding two unrestricted
 *     green mana to the controller's pool.
 *  2. Ward {2}: when an opponent targets HulkingRaptor with a spell and has enough mana to pay,
 *     the engine presents a [SelectManaSourcesDecision] for the {2} ward cost.
 *  3. Ward counters the targeting spell when the opponent cannot pay the {2} cost.
 */
class HulkingRaptorScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HulkingRaptor)
        return driver
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: First-main-phase trigger adds {G}{G} to the controller's pool
    // ─────────────────────────────────────────────────────────────────────────
    test("first-main trigger adds two unrestricted green mana to the controller's pool") {
        val driver = newDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.player1

        // Put Hulking Raptor on the battlefield before the precombat main phase begins so
        // the FirstMainPhase trigger fires when the phase transitions.
        driver.putCreatureOnBattlefield(player, "Hulking Raptor")

        // Advance to the precombat main step; the trigger fires and goes on the stack.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Both players pass priority → trigger resolves, adding {G}{G}.
        driver.bothPass()

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()
        pool.shouldNotBeNull()
        pool.green shouldBe 2
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Ward {2} prompts the opponent when they can afford to pay
    // ─────────────────────────────────────────────────────────────────────────
    test("ward prompts the targeting player with a mana selection when they can pay {2}") {
        val driver = newDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.player1
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls Hulking Raptor.
        val raptor = driver.putCreatureOnBattlefield(opponent, "Hulking Raptor")

        // Active player has {R} for Lightning Bolt and three Mountains to pay ward {2}.
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.giveMana(activePlayer, Color.RED, 1)

        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(raptor)))

        // Ward trigger resolves — active player should see a mana-selection prompt.
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.playerId shouldBe activePlayer
        decision.canDecline shouldBe true
        decision.requiredCost shouldBe "{2}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Ward {2} counters the targeting spell when the opponent cannot pay
    // ─────────────────────────────────────────────────────────────────────────
    test("ward counters the targeting spell when the caster cannot pay {2}") {
        val driver = newDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val activePlayer = driver.player1
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls Hulking Raptor.
        val raptor = driver.putCreatureOnBattlefield(opponent, "Hulking Raptor")

        // Active player has exactly {R} for Lightning Bolt — nothing left for ward {2}.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(raptor)))

        // Ward trigger fires; the caster cannot pay {2} — spell is countered immediately.
        driver.bothPass()

        // No pending decision — executor went straight to counter.
        driver.pendingDecision shouldBe null

        // Resolve any remaining items.
        driver.bothPass()

        // Raptor survives; Bolt was countered.
        driver.findPermanent(opponent, "Hulking Raptor").shouldNotBeNull()
        driver.getGraveyardCardNames(activePlayer).contains("Lightning Bolt") shouldBe true
    }
})
