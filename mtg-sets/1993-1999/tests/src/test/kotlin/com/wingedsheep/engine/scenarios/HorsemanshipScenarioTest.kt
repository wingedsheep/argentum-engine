package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.ShuCavalry
import com.wingedsheep.mtg.sets.definitions.ptk.cards.ShuDefender
import com.wingedsheep.mtg.sets.definitions.ptk.cards.WuLightCavalry
import com.wingedsheep.mtg.sets.definitions.ptk.cards.ZuoCiTheMockingSage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for horsemanship (CR 702.31) — Portal Three Kingdoms' signature evasion keyword.
 *
 * `Keyword.HORSEMANSHIP` and [com.wingedsheep.engine.mechanics.combat.rules.HorsemanshipRule] both
 * predate this test, but until the PTK sweep **no card in the corpus declared the keyword**, so
 * nothing ever exercised the rule end to end. That is the display-only-keyword failure mode
 * (cascade, modular, annihilator): the line renders and the engine ignores it. These tests prove
 * horsemanship is live in both directions.
 *
 * Zuo Ci is the mirror case — a static "can't be blocked by creatures with horsemanship" rather
 * than the keyword itself, and the only PTK card that reads the keyword off a *blocker*.
 */
class HorsemanshipScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WuLightCavalry)
        driver.registerCard(ShuCavalry)
        driver.registerCard(ShuDefender)
        driver.registerCard(ZuoCiTheMockingSage)
        return driver
    }

    test("a horsemanship attacker can only be blocked by a creature with horsemanship") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(me, "Wu Light Cavalry")
        driver.removeSummoningSickness(attacker)
        val plainBlocker = driver.putCreatureOnBattlefield(opponent, "Shu Defender")
        val horseBlocker = driver.putCreatureOnBattlefield(opponent, "Shu Cavalry")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("Shu Defender has no horsemanship, so it cannot block Wu Light Cavalry") {
            driver.declareBlockers(opponent, mapOf(plainBlocker to listOf(attacker))).isSuccess shouldBe false
        }
        withClue("Shu Cavalry does have horsemanship, so the block is legal") {
            driver.declareBlockers(opponent, mapOf(horseBlocker to listOf(attacker))).isSuccess shouldBe true
        }
    }

    test("horsemanship restricts nothing when the attacker lacks it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(me, "Shu Defender")
        driver.removeSummoningSickness(attacker)
        val horseBlocker = driver.putCreatureOnBattlefield(opponent, "Shu Cavalry")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("a horsemanship creature may block anything; the keyword only gates the attacker") {
            driver.declareBlockers(opponent, mapOf(horseBlocker to listOf(attacker))).isSuccess shouldBe true
        }
    }

    test("Zuo Ci inverts it: a horsemanship creature is the one that cannot block") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(me, "Zuo Ci, the Mocking Sage")
        driver.removeSummoningSickness(attacker)
        val horseBlocker = driver.putCreatureOnBattlefield(opponent, "Shu Cavalry")
        val plainBlocker = driver.putCreatureOnBattlefield(opponent, "Shu Defender")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("Zuo Ci can't be blocked by creatures with horsemanship") {
            driver.declareBlockers(opponent, mapOf(horseBlocker to listOf(attacker))).isSuccess shouldBe false
        }
        withClue("a creature without horsemanship blocks Zuo Ci normally") {
            driver.declareBlockers(opponent, mapOf(plainBlocker to listOf(attacker))).isSuccess shouldBe true
        }
    }
})
