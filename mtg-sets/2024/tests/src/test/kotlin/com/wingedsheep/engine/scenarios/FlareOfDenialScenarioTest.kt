package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.FlareOfDenial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flare of Denial {1}{U}{U} — Instant (MH3).
 *
 * "You may sacrifice a nontoken blue creature rather than pay this spell's mana cost.
 *  Counter target spell."
 */
class FlareOfDenialScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlareOfDenial))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Puts a Lightning Bolt aimed at the opponent on the stack; the caster keeps priority. */
    fun castBolt(driver: GameTestDriver, caster: EntityId, at: EntityId): EntityId {
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Player(at))).error shouldBe null
        return bolt
    }

    fun castBySacrificing(driver: GameTestDriver, player: EntityId, flare: EntityId, fodder: EntityId, spell: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = flare,
                targets = listOf(ChosenTarget.Spell(spell)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        )

    test("sacrificing a nontoken blue creature counters target spell for free") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val walker = driver.putCreatureOnBattlefield(me, "Island Walker")
        val flare = driver.putCardInHand(me, "Flare of Denial")
        val bolt = castBolt(driver, me, opponent)

        castBySacrificing(driver, me, flare, walker, bolt).error shouldBe null
        withClue("the blue creature paid the alternative cost") {
            driver.findPermanent(me, "Island Walker") shouldBe null
        }
        driver.bothPass()

        withClue("the targeted spell is countered") {
            driver.getLifeTotal(opponent) shouldBe 20
            driver.getGraveyard(me) shouldContain bolt
            driver.stackSize shouldBe 0
        }
    }

    test("hard-cast for its mana cost counters target spell") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val flare = driver.putCardInHand(me, "Flare of Denial")
        val bolt = castBolt(driver, me, opponent)
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpellWithTargets(me, flare, listOf(ChosenTarget.Spell(bolt))).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 20
        driver.getGraveyard(me) shouldContain bolt
    }

    test("a non-blue creature can't pay the alternative cost") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val black = driver.putCreatureOnBattlefield(me, "Black Creature")
        val flare = driver.putCardInHand(me, "Flare of Denial")
        val bolt = castBolt(driver, me, opponent)

        castBySacrificing(driver, me, flare, black, bolt).error shouldNotBe null
        driver.findPermanent(me, "Black Creature") shouldNotBe null
    }
})
