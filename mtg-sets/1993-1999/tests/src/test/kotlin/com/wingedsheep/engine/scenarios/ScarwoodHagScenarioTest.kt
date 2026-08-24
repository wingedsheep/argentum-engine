package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.ScarwoodHag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scarwood Hag — a give/take pair on forestwalk.
 *
 * The give half is proved through blocking rather than by reading a keyword flag: forestwalk that
 * projects but doesn't reach the blocker check would be a silent no-op. The take half is aimed at a
 * creature with *printed* forestwalk, which is the case that matters — removing a keyword you just
 * granted proves much less than removing one the card came with.
 */
class ScarwoodHagScenarioTest : FunSpec({

    val grantAbilityId = ScarwoodHag.activatedAbilities[0].id
    val removeAbilityId = ScarwoodHag.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ScarwoodHag)
        return driver
    }

    test("granted forestwalk makes an attacker unblockable while the defender has a Forest") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hag = driver.putCreatureOnBattlefield(me, "Scarwood Hag")
        driver.removeSummoningSickness(hag)
        val attacker = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putLandOnBattlefield(opponent, "Forest")
        driver.giveMana(me, Color.GREEN, 4)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = hag,
                abilityId = grantAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, attacker)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.projectedState.hasKeyword(attacker, Keyword.FORESTWALK) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("the defender controls a Forest, so forestwalk shuts the block down") {
            driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe false
        }
    }

    test("the second ability strips printed forestwalk for the turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hag = driver.putCreatureOnBattlefield(me, "Scarwood Hag")
        driver.removeSummoningSickness(hag)
        // Rushwood Dryad has forestwalk printed on it — nothing granted it.
        val walker = driver.putCreatureOnBattlefield(opponent, "Rushwood Dryad")

        withClue("it starts with its printed forestwalk") {
            driver.state.projectedState.hasKeyword(walker, Keyword.FORESTWALK) shouldBe true
        }

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = hag,
                abilityId = removeAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, walker)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("and the Hag takes it away for the turn") {
            driver.state.projectedState.hasKeyword(walker, Keyword.FORESTWALK) shouldBe false
        }
    }
})
