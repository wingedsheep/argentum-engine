package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.FlareOfFortitude
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flare of Fortitude {2}{W}{W} — Instant (MH3).
 *
 * "You may sacrifice a nontoken white creature rather than pay this spell's mana cost.
 *  Until end of turn, your life total can't change, and permanents you control gain hexproof and
 *  indestructible."
 *
 * "Your life total can't change" is a life-gain lock plus a life-loss lock (CR 119.7–8), both
 * scoped to the turn.
 */
class FlareOfFortitudeScenarioTest : FunSpec({

    val GainFiveLife = CardDefinition.instant(
        name = "Gain Five Life",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "You gain 5 life.",
        script = CardScript.spell(effect = GainLifeEffect(5)),
    )

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlareOfFortitude, GainFiveLife))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castBySacrificing(driver: GameTestDriver, player: EntityId, flare: EntityId, fodder: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = flare,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        )

    fun bolt(driver: GameTestDriver, caster: EntityId, target: ChosenTarget) =
        driver.putCardInHand(caster, "Lightning Bolt").let { bolt ->
            driver.giveMana(caster, Color.RED, 1)
            driver.castSpellWithTargets(caster, bolt, listOf(target))
        }

    test("sacrificing a white creature: your life total can't change and your permanents are protected this turn") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val keeper = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val flare = driver.putCardInHand(me, "Flare of Fortitude")

        castBySacrificing(driver, me, flare, lions).error shouldBe null
        driver.bothPass()

        withClue("the white creature paid the alternative cost") {
            driver.findPermanent(me, "Savannah Lions") shouldBe null
        }

        withClue("damage doesn't lower the life total") {
            bolt(driver, me, ChosenTarget.Player(me)).error shouldBe null
            driver.bothPass()
            driver.getLifeTotal(me) shouldBe 20
        }

        withClue("life gain doesn't raise it") {
            val gain = driver.putCardInHand(me, "Gain Five Life")
            driver.giveMana(me, Color.WHITE, 1)
            driver.castSpell(me, gain).error shouldBe null
            driver.bothPass()
            driver.getLifeTotal(me) shouldBe 20
        }

        withClue("and indestructible — your own Doom Blade doesn't kill a nonblack creature") {
            val blade = driver.putCardInHand(me, "Doom Blade")
            driver.giveMana(me, Color.BLACK, 2)
            driver.castSpellWithTargets(me, blade, listOf(ChosenTarget.Permanent(keeper))).error shouldBe null
            driver.bothPass()
            driver.findPermanent(me, "Centaur Courser") shouldNotBe null
        }

        withClue("permanents you control have hexproof — an opponent can't target them") {
            driver.passPriority(me)
            val error = bolt(driver, opponent, ChosenTarget.Permanent(keeper)).error
            error shouldNotBe null
            error shouldNotBe "You don't have priority"
        }

        withClue("the lock ends with the turn") {
            driver.passPriorityUntil(Step.UPKEEP)
            bolt(driver, opponent, ChosenTarget.Player(me)).error shouldBe null
            driver.bothPass()
            driver.getLifeTotal(me) shouldBe 17
        }
    }

    test("a black creature can't pay the alternative cost") {
        val driver = newDriver()
        val me = driver.player1

        val black = driver.putCreatureOnBattlefield(me, "Black Creature")
        val flare = driver.putCardInHand(me, "Flare of Fortitude")

        castBySacrificing(driver, me, flare, black).error shouldNotBe null
        driver.findPermanent(me, "Black Creature") shouldNotBe null
    }
})
