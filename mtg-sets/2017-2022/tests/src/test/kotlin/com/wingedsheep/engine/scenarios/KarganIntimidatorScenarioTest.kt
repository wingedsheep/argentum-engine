package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.KarganIntimidator
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kargan Intimidator (ZNR #145).
 *
 * The first activated ability built on "choose one that hasn't been chosen this turn": each mode is
 * announced at activation and spent for the rest of the turn, while the other modes stay available.
 * Also covers the battlefield-wide "Cowards can't block Warriors" read from the attacker's side,
 * together with the "becomes a Coward" mode that turns a would-be blocker into a Coward.
 */
class KarganIntimidatorScenarioTest : FunSpec({

    val pump = 0
    val coward = 1
    val trample = 2
    val abilityId = KarganIntimidator.activatedAbilities.single().id

    fun start(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + KarganIntimidator)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activate(player: EntityId, source: EntityId, mode: Int, target: EntityId? = null) =
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = source,
                abilityId = abilityId,
                targets = listOfNotNull(target).map { ChosenTarget.Permanent(it) },
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(listOfNotNull(target).map { ChosenTarget.Permanent(it) }),
            )
        )

    test("each mode can be chosen once per turn; the others stay available") {
        val driver = start()
        val active = driver.activePlayer!!
        val kargan = driver.putCreatureOnBattlefield(active, "Kargan Intimidator")
        driver.removeSummoningSickness(kargan)
        driver.giveColorlessMana(active, 3)

        driver.activate(active, kargan, pump).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.getPower(kargan) shouldBe 4
        driver.state.projectedState.getToughness(kargan) shouldBe 2

        withClue("the +1/+1 mode was already chosen this turn") {
            driver.activate(active, kargan, pump).error shouldNotBe null
        }

        driver.activate(active, kargan, trample, target = kargan).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.hasKeyword(kargan, Keyword.TRAMPLE) shouldBe true
        driver.state.projectedState.getPower(kargan) shouldBe 4
    }

    test("a creature made a Coward can't block a Warrior, while a non-Coward still can") {
        val driver = start()
        val active = driver.activePlayer!!
        val opp = driver.getOpponent(active)
        val kargan = driver.putCreatureOnBattlefield(active, "Kargan Intimidator")
        driver.removeSummoningSickness(kargan)
        val cowardBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val braveBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.giveColorlessMana(active, 1)

        driver.activate(active, kargan, coward, target = cowardBears).error shouldBe null
        driver.bothPass()
        withClue("becoming a Coward replaces the creature's other creature types") {
            driver.state.projectedState.getSubtypes(cowardBears) shouldBe setOf("Coward")
        }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(kargan), opp).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("Cowards can't block Warriors") {
            driver.declareBlockers(opp, mapOf(cowardBears to listOf(kargan))).error shouldNotBe null
        }
        driver.declareBlockers(opp, mapOf(braveBears to listOf(kargan))).error shouldBe null
    }
})
