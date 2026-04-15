package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.DragonhawkFatesTempest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Dragonhawk, Fate's Tempest.
 *
 * Dragonhawk, Fate's Tempest {3}{R}{R}
 * Legendary Creature — Bird Dragon
 * 5/5
 * Flying
 *
 * Whenever Dragonhawk enters or attacks, exile the top X cards of your library,
 * where X is the number of creatures you control with power 4 or greater. You may
 * play those cards until your next end step. At the beginning of your next end step,
 * Dragonhawk deals 2 damage to each opponent for each of those cards that are still exiled.
 */
class DragonhawkFatesTempestTest : FunSpec({

    val bigCreature = CardDefinition.creature("Test Big Creature", ManaCost.parse("{2}{G}{G}"), emptySet(), 4, 4)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DragonhawkFatesTempest, bigCreature))
        return driver
    }

    test("ETB exiles cards and delayed trigger deals damage at THIS turn's end step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Dragonhawk is 5/5 → counts toward X. With just Dragonhawk, X = 1.
        driver.putCardOnTopOfLibrary(p1, "Mountain")

        val dragonhawkId = driver.putCardInHand(p1, "Dragonhawk, Fate's Tempest")
        driver.giveMana(p1, Color.RED, 2)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, dragonhawkId)

        // Resolve spell → ETB trigger → resolve ETB (exiles 1 card)
        driver.passPriority(p1)
        driver.passPriority(p2)
        driver.passPriority(p1)
        driver.passPriority(p2)

        driver.getExileCardNames(p1).size shouldBe 1
        driver.getLifeTotal(p2) shouldBe 20

        // "Your next end step" is the current turn's end step — delayed trigger fires there.
        driver.passPriorityUntil(Step.END, maxPasses = 200)

        // Resolve the delayed trigger on the stack
        driver.passPriority(p1)
        driver.passPriority(p2)

        // 1 still exiled × 2 damage = 2 damage to opponent, in the SAME turn Dragonhawk entered
        driver.getLifeTotal(p2) shouldBe 18
        driver.state.activePlayerId shouldBe p1
    }

    test("no damage if all exiled cards were played before the end step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardOnTopOfLibrary(p1, "Mountain")

        val dragonhawkId = driver.putCardInHand(p1, "Dragonhawk, Fate's Tempest")
        driver.giveMana(p1, Color.RED, 2)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, dragonhawkId)

        driver.passPriority(p1)
        driver.passPriority(p2)
        driver.passPriority(p1)
        driver.passPriority(p2)

        driver.getExileCardNames(p1).size shouldBe 1

        // Play the exiled Mountain (impulse land play from exile)
        val exiledMountain = driver.getExile(p1).first()
        driver.playLand(p1, exiledMountain)
        driver.getExileCardNames(p1).size shouldBe 0

        // Advance to this turn's end step — delayed trigger fires with 0 cards still exiled
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.passPriority(p1)
        driver.passPriority(p2)

        // 0 cards exiled → 0 damage
        driver.getLifeTotal(p2) shouldBe 20
    }

    test("X equals number of creatures with power 4 or greater") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 4/4 creature on battlefield before Dragonhawk
        driver.putCreatureOnBattlefield(p1, "Test Big Creature")

        // 2 cards on top (X = 2: Dragonhawk 5/5 + Big Creature 4/4)
        driver.putCardOnTopOfLibrary(p1, "Mountain")
        driver.putCardOnTopOfLibrary(p1, "Mountain")

        val dragonhawkId = driver.putCardInHand(p1, "Dragonhawk, Fate's Tempest")
        driver.giveMana(p1, Color.RED, 2)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, dragonhawkId)

        driver.passPriority(p1)
        driver.passPriority(p2)
        driver.passPriority(p1)
        driver.passPriority(p2)

        driver.getExileCardNames(p1).size shouldBe 2

        // Delayed trigger fires at THIS turn's end step
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.passPriority(p1)
        driver.passPriority(p2)

        // 2 still exiled × 2 damage = 4 damage
        driver.getLifeTotal(p2) shouldBe 16
    }

    test("impulse-play window closes after the current turn, not a turn later") {
        // "Until your next end step" aligns with the delayed trigger timing: once the
        // current turn's end step has happened, the exiled cards can no longer be played.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardOnTopOfLibrary(p1, "Mountain")

        val dragonhawkId = driver.putCardInHand(p1, "Dragonhawk, Fate's Tempest")
        driver.giveMana(p1, Color.RED, 2)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, dragonhawkId)

        driver.passPriority(p1)
        driver.passPriority(p2)
        driver.passPriority(p1)
        driver.passPriority(p2)

        val exiledMountain = driver.getExile(p1).first()
        // Impulse window is open this turn (we just don't play it yet).
        driver.state.getEntity(exiledMountain)
            ?.get<com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent>() shouldNotBe null

        // Advance through this turn's end step (delayed trigger fires), then past cleanup,
        // into P2's turn, and back to P1's next main phase.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.passPriority(p1)
        driver.passPriority(p2)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        if (driver.state.activePlayerId != p1) {
            driver.passPriorityUntil(Step.END, maxPasses = 200)
            driver.passPriority(p1)
            driver.passPriority(p2)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        }

        // On P1's next turn's main phase, the impulse component should be gone.
        driver.state.getEntity(exiledMountain)
            ?.get<com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent>() shouldBe null
    }

    test("attack trigger's damage resolves at the same turn's end step") {
        // Regression: "your next end step" means the next upcoming end step on your turn.
        // When Dragonhawk attacks during combat, the current turn's end step has not yet
        // occurred — the delayed trigger must fire this turn, not a turn later.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        val dragonhawkId = driver.putCreatureOnBattlefield(p1, "Dragonhawk, Fate's Tempest")
        driver.removeSummoningSickness(dragonhawkId)

        // Attack triggers on declare attackers. Exile 1 card (X=1 from Dragonhawk itself).
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(dragonhawkId), p2)

        // Resolve the attack trigger
        driver.bothPass()
        driver.getExileCardNames(p1).size shouldBe 1
        driver.getLifeTotal(p2) shouldBe 20

        // Advance to this turn's end step — delayed trigger fires HERE, not next turn.
        // Flying Dragonhawk also deals 5 combat damage along the way.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.state.activePlayerId shouldBe p1
        val lifeBeforeDelayedTrigger = driver.getLifeTotal(p2)

        driver.passPriority(p1)
        driver.passPriority(p2)

        // 2 damage from the delayed trigger resolves in the same turn Dragonhawk attacked
        driver.state.activePlayerId shouldBe p1
        driver.getLifeTotal(p2) shouldBe lifeBeforeDelayedTrigger - 2
        driver.state.delayedTriggers.size shouldBe 0
    }
})
