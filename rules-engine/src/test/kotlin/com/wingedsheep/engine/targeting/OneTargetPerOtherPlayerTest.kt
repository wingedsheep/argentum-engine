package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the "one per other player" distribution shape —
 * "For each other player, exile **up to one** target creature that player controls"
 * (Kaya, Spirits' Justice's −2).
 *
 * It is spelled as three orthogonal knobs on one [TargetCreature] requirement rather than a bespoke
 * type, and this test pins each one:
 *
 *  - `dynamicMaxCount = DynamicAmount.PlayerCount(Player.EachOpponent)` — how many players are in
 *    scope, resolved from the live table;
 *  - `differentControllers = true` — at most one creature each;
 *  - `optional = true` — the "up to", so hitting fewer than the maximum is legal.
 *
 * CR 601.2c chooses all of a spell or ability's targets together, and the card's own ruling says
 * "You choose all targets for Kaya, Spirits' Justice's last ability."
 */
class OneTargetPerOtherPlayerTest : FunSpec({

    val bear = card("Per Player Target Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    /** The −2's second requirement, exactly as the card declares it. */
    val perOtherPlayer = TargetCreature(
        filter = TargetFilter.CreatureOpponentControls,
        optional = true,
        dynamicMaxCount = DynamicAmount.PlayerCount(Player.EachOpponent),
        differentControllers = true,
        id = "one target creature each other player controls",
    )

    fun driverWith(seats: Int): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bear))
        driver.initMultiplayer(decks = List(seats) { Deck.of("Forest" to 40) })
        return driver
    }

    context("DynamicAmount.PlayerCount") {

        test("counts the opponents at the table, not the whole table") {
            val fourSeats = driverWith(4)
            val evaluator = DynamicAmountEvaluator()
            val context = EffectContext(sourceId = null, controllerId = fourSeats.player1)

            withClue("three other players in a four-player game") {
                evaluator.evaluate(
                    fourSeats.state,
                    DynamicAmount.PlayerCount(Player.EachOpponent),
                    context,
                ) shouldBe 3
            }
            withClue("Player.Each counts you as well") {
                evaluator.evaluate(
                    fourSeats.state,
                    DynamicAmount.PlayerCount(Player.Each),
                    context,
                ) shouldBe 4
            }
        }

        test("a two-player game leaves exactly one target available") {
            val heads = driverWith(2)
            DynamicAmountEvaluator().evaluate(
                heads.state,
                DynamicAmount.PlayerCount(Player.EachOpponent),
                EffectContext(sourceId = null, controllerId = heads.player1),
            ) shouldBe 1
        }
    }

    context("the requirement's resolved cap") {

        test("the enumerator caps the requirement at the number of other players") {
            val driver = driverWith(3)
            val seats = driver.state.activePlayers
            seats.drop(1).forEach { driver.putCreatureOnBattlefield(it, "Per Player Target Bear") }

            val info = TargetEnumerationUtils(PredicateEvaluator())
                .buildTargetInfos(driver.state, driver.player1, listOf(perOtherPlayer))
                .single()

            withClue("two opponents => up to two targets") { info.maxTargets shouldBe 2 }
            withClue("\"up to\" means picking none is legal") { info.minTargets shouldBe 0 }
        }
    }

    context("differentControllers (CR 601.2c)") {

        fun validate(driver: GameTestDriver, targets: List<ChosenTarget>) =
            TargetValidator().validateTargets(
                state = driver.state,
                targets = targets,
                requirements = listOf(perOtherPlayer),
                casterId = driver.player1,
                // The shape under test is a planeswalker's loyalty ability (Kaya's −2), so the
                // spells-only restrictions (Lurker) must not apply to it.
                targetingSourceType = TargetingSourceType.ABILITY,
            )

        test("one creature from each of two opponents is legal") {
            val driver = driverWith(3)
            val seats = driver.state.activePlayers
            val first = driver.putCreatureOnBattlefield(seats[1], "Per Player Target Bear")
            val second = driver.putCreatureOnBattlefield(seats[2], "Per Player Target Bear")

            validate(driver, listOf(ChosenTarget.Permanent(first), ChosenTarget.Permanent(second)))
                .shouldBeNull()
        }

        test("two creatures the SAME player controls is rejected") {
            val driver = driverWith(3)
            val seats = driver.state.activePlayers
            val one = driver.putCreatureOnBattlefield(seats[1], "Per Player Target Bear")
            val two = driver.putCreatureOnBattlefield(seats[1], "Per Player Target Bear")

            withClue("\"up to one ... that player controls\" is one per player, not two from one") {
                validate(driver, listOf(ChosenTarget.Permanent(one), ChosenTarget.Permanent(two)))
                    .shouldNotBeNull()
            }
        }

        test("a single target is always fine, and so is none") {
            val driver = driverWith(3)
            val seats = driver.state.activePlayers
            val only = driver.putCreatureOnBattlefield(seats[1], "Per Player Target Bear")

            validate(driver, listOf(ChosenTarget.Permanent(only))).shouldBeNull()
            validate(driver, emptyList()).shouldBeNull()
        }
    }
})
