package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.TelJiladChosen
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tel-Jilad Chosen — {1}{G} Creature — Elf Warrior 2/1 (Mirrodin #132)
 *
 * "Protection from artifacts"
 *
 * Modelled as `KeywordAbility.Protection(ProtectionScope.CardType("Artifact"))`, projected as the
 * `PROTECTION_FROM_CARDTYPE_ARTIFACT` keyword. "Artifact" is a new instantiation of the card-type
 * protection scope, so this test proves the DEBT legs the engine actually enforces for it —
 * **B**locking and **D**amage — rather than trusting the generic wiring.
 */
class TelJiladChosenScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TelJiladChosen)
        return driver
    }

    test("the protection keyword is projected onto the permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val p1 = driver.activePlayer!!

        val chosen = driver.putCreatureOnBattlefield(p1, "Tel-Jilad Chosen")

        projector.project(driver.state)
            .hasKeyword(chosen, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe true
        projector.getProjectedPower(driver.state, chosen) shouldBe 2
        projector.getProjectedToughness(driver.state, chosen) shouldBe 1
    }

    test("an artifact creature can't block it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val chosen = driver.putCreatureOnBattlefield(attacker, "Tel-Jilad Chosen") // 2/1
        val golem = driver.putCreatureOnBattlefield(defender, "Artifact Creature") // 2/2 artifact
        driver.removeSummoningSickness(chosen)
        driver.removeSummoningSickness(golem)

        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn // didn't sail past this turn's combat
        driver.declareAttackers(attacker, listOf(chosen), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // The artifact Golem is not a legal blocker for a creature with protection from artifacts.
        driver.declareBlockers(defender, mapOf(golem to listOf(chosen))).isSuccess shouldBe false
    }

    test("a nonartifact creature can still block it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val chosen = driver.putCreatureOnBattlefield(attacker, "Tel-Jilad Chosen") // 2/1
        val courser = driver.putCreatureOnBattlefield(defender, "Centaur Courser") // 3/3 nonartifact
        driver.removeSummoningSickness(chosen)
        driver.removeSummoningSickness(courser)

        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn
        driver.declareAttackers(attacker, listOf(chosen), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(defender, mapOf(courser to listOf(chosen))).isSuccess shouldBe true
    }

    test("combat damage from an artifact creature is prevented") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        // The *active* player is the one attacking with the artifact creature, so the very next
        // declare-attackers step is theirs — no risk of sailing into a later turn.
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val golem = driver.putCreatureOnBattlefield(attacker, "Artifact Creature") // 2/2 artifact
        val chosen = driver.putCreatureOnBattlefield(defender, "Tel-Jilad Chosen") // 2/1
        driver.removeSummoningSickness(golem)
        driver.removeSummoningSickness(chosen)

        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn
        driver.declareAttackers(attacker, listOf(golem), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(chosen to listOf(golem))).isSuccess shouldBe true
        driver.passPriorityUntil(Step.END_COMBAT)

        // The Golem's 2 damage would be lethal to a 2/1, but protection prevents all of it.
        // The Chosen's own 2 damage is unaffected and kills the 2/2 Golem.
        driver.findPermanent(defender, "Tel-Jilad Chosen") shouldNotBe null
        driver.findPermanent(attacker, "Artifact Creature") shouldBe null
    }
})
