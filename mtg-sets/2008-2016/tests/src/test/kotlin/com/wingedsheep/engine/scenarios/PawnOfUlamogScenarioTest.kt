package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.roe.cards.PawnOfUlamog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pawn of Ulamog (ROE) — {1}{B}{B} Creature — Vampire Shaman, 2/2.
 *
 * "Whenever this creature or another nontoken creature you control dies, you may create a 0/1
 *  colorless Eldrazi Spawn creature token. It has "Sacrifice this token: Add {C}.""
 *
 * Proves the two halves of the trigger: another nontoken creature dying offers the Spawn; Pawn's
 * own death offers it too (leaves-the-battlefield look-back); declining makes nothing; and a token
 * creature dying (the Spawn itself) does not trigger.
 */
class PawnOfUlamogScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PawnOfUlamog) + PredefinedTokens.allTokens)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.spawnCount(playerId: EntityId): Int =
        getPermanents(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == "Eldrazi Spawn" }

    /** Opponent kills [victim] with a Lightning Bolt so it dies to a real death event. */
    fun GameTestDriver.boltToDeath(victim: EntityId) {
        val you = activePlayer!!
        val opponent = getOpponent(you)
        giveMana(opponent, Color.RED, 1)
        val bolt = putCardInHand(opponent, "Lightning Bolt")
        passPriority(you)
        castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
    }

    /** Resolve the stack, answering every "may" with [accept]; returns how many were offered. */
    fun GameTestDriver.resolveAll(accept: Boolean): Int {
        var offers = 0
        var guard = 0
        while ((stackSize > 0 || state.pendingDecision != null) && guard < 40) {
            when (val pending = state.pendingDecision) {
                is YesNoDecision -> {
                    offers++
                    submitYesNo(pending.playerId, accept)
                }
                else -> bothPass()
            }
            guard++
        }
        return offers
    }

    test("another nontoken creature you control dying creates an Eldrazi Spawn") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        driver.putCreatureOnBattlefield(you, "Pawn of Ulamog")
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        driver.boltToDeath(bears)
        driver.resolveAll(accept = true) shouldBe 1

        driver.spawnCount(you) shouldBe 1
    }

    test("Pawn of Ulamog's own death also creates an Eldrazi Spawn") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val pawn = driver.putCreatureOnBattlefield(you, "Pawn of Ulamog")

        driver.boltToDeath(pawn)
        driver.resolveAll(accept = true) shouldBe 1

        driver.spawnCount(you) shouldBe 1
    }

    test("declining the may creates nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        driver.putCreatureOnBattlefield(you, "Pawn of Ulamog")
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        driver.boltToDeath(bears)
        driver.resolveAll(accept = false) shouldBe 1

        driver.spawnCount(you) shouldBe 0
    }

    test("a token creature you control dying does not trigger") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        driver.putCreatureOnBattlefield(you, "Pawn of Ulamog")
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.boltToDeath(bears)
        driver.resolveAll(accept = true)
        driver.spawnCount(you) shouldBe 1

        val spawn = driver.findPermanent(you, "Eldrazi Spawn")!!
        driver.boltToDeath(spawn)
        driver.resolveAll(accept = true) shouldBe 0

        driver.spawnCount(you) shouldBe 0
    }
})
