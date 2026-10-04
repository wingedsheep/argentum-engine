package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gpt.cards.GhostWarden
import com.wingedsheep.mtg.sets.definitions.j22.cards.AgrusKosEternalSoldier
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Agrus Kos, Eternal Soldier — {3}{W} Legendary Creature — Spirit Soldier, 3/4 (J22).
 *
 *   Vigilance
 *   Whenever Agrus Kos becomes the target of an ability that targets only it, you may pay {1}{R/W}.
 *   If you do, copy that ability for each other creature you control that ability could target.
 *   Each copy targets a different one of those creatures.
 */
class AgrusKosEternalSoldierScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AgrusKosEternalSoldier, GhostWarden, GrizzlyBears))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolve everything, answering the may-pay with [pay] and auto-paying its mana. */
    fun GameTestDriver.resolveAll(pay: Boolean): Int {
        var prompts = 0
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            when (val decision = state.pendingDecision) {
                is YesNoDecision -> { prompts++; submitYesNo(decision.playerId, pay) }
                is SelectManaSourcesDecision -> submitManaAutoPayOrDecline(decision.playerId, autoPay = true)
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
        return prompts
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id)!!

    fun GameTestDriver.board(): Triple<EntityId, EntityId, EntityId> {
        val me = activePlayer!!
        val agrus = putCreatureOnBattlefield(me, "Agrus Kos, Eternal Soldier")
        val warden = putCreatureOnBattlefield(me, "Ghost Warden")
        removeSummoningSickness(warden)
        val bear = putCreatureOnBattlefield(me, "Grizzly Bears")
        putLandOnBattlefield(me, "Plains")
        putLandOnBattlefield(me, "Plains")
        return Triple(agrus, warden, bear)
    }

    fun GameTestDriver.wardenTargets(warden: EntityId, target: EntityId) = submit(
        ActivateAbility(
            playerId = activePlayer!!,
            sourceId = warden,
            abilityId = GhostWarden.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(target))
        )
    )

    test("has vigilance") {
        val driver = createDriver()
        val agrus = driver.putCreatureOnBattlefield(driver.activePlayer!!, "Agrus Kos, Eternal Soldier")
        driver.state.projectedState.hasKeyword(agrus, Keyword.VIGILANCE) shouldBe true
    }

    test("paying copies the ability for each other creature you control, the source included") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val theirBear = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val (agrus, warden, bear) = driver.board()

        driver.wardenTargets(warden, agrus).error shouldBe null
        driver.resolveAll(pay = true) shouldBe 1

        driver.power(agrus) shouldBe 4
        driver.power(bear) shouldBe 3
        withClue("Ghost Warden is another creature its own ability could target") { driver.power(warden) shouldBe 2 }
        withClue("an opponent's creature is not one of yours") { driver.power(theirBear) shouldBe 2 }
        withClue("{1}{R/W} was paid") { driver.getUntappedLands(me) shouldHaveSize 0 }
    }

    test("declining the payment leaves only the original ability") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val (agrus, warden, bear) = driver.board()

        driver.wardenTargets(warden, agrus).error shouldBe null
        driver.resolveAll(pay = false) shouldBe 1

        driver.power(agrus) shouldBe 4
        driver.power(bear) shouldBe 2
        driver.power(warden) shouldBe 1
        driver.getUntappedLands(me) shouldHaveSize 2
    }

    test("an ability targeting another creature doesn't trigger it") {
        val driver = createDriver()
        val (agrus, warden, bear) = driver.board()

        driver.wardenTargets(warden, bear).error shouldBe null
        driver.resolveAll(pay = true) shouldBe 0

        driver.power(bear) shouldBe 3
        driver.power(agrus) shouldBe 3
    }

    test("an opponent's ability is copied for Agrus Kos's controller, onto that player's creatures") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val theirWarden = driver.putCreatureOnBattlefield(opponent, "Ghost Warden")
        driver.removeSummoningSickness(theirWarden)
        val (agrus, warden, bear) = driver.board()

        driver.passPriority(me)
        driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = theirWarden,
                abilityId = GhostWarden.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(agrus))
            )
        ).error shouldBe null
        driver.resolveAll(pay = true) shouldBe 1

        driver.power(agrus) shouldBe 4
        withClue("the copies are Agrus Kos's controller's, aimed at that player's creatures") {
            driver.power(warden) shouldBe 2
            driver.power(bear) shouldBe 3
        }
        withClue("the opponent's creature is not one of yours") { driver.power(theirWarden) shouldBe 1 }
        withClue("Agrus Kos's controller paid {1}{R/W}") { driver.getUntappedLands(me) shouldHaveSize 0 }
    }

    test("a spell targeting only Agrus Kos doesn't trigger it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val (agrus, _, bear) = driver.board()
        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN)

        driver.castSpellWithTargets(me, growth, listOf(ChosenTarget.Permanent(agrus))).error shouldBe null
        driver.resolveAll(pay = true) shouldBe 0

        driver.power(agrus) shouldBe 6
        driver.power(bear) shouldBe 2
    }
})
