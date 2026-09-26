package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The legend rule (CR 704.5j) honours a `LegendRuleDoesNotApplyTo` wrapped in a
 * `ConditionalStaticAbility` only while the condition holds at the state-based-action check —
 * printed on a permanent or granted for a duration.
 */
class ConditionalLegendRuleExemptionTest : FunSpec({

    val twinsName = "Test Legendary Twins"
    val twins = GameObjectFilter.Permanent.named(twinsName)
    val exactlyTwoTwins = Conditions.CompareAmounts(
        DynamicAmounts.battlefield(Player.Each, twins).count(),
        ComparisonOperator.EQ,
        2
    )

    val legendaryTwins = card(twinsName) {
        manaCost = "{R}"
        typeLine = "Legendary Creature — Human"
        power = 1
        toughness = 1
        staticAbility {
            ability = ConditionalStaticAbility(LegendRuleDoesNotApplyTo(twins), exactlyTwoTwins)
        }
    }

    val plainLegend = card("Test Plain Legend") {
        manaCost = "{R}"
        typeLine = "Legendary Creature — Human"
        power = 1
        toughness = 1
    }

    /** "The legend rule doesn't apply to permanents you control this turn, as long as [namedCount] ≥ 1." */
    fun conditionalEcho(cardName: String, requiredName: String) = card(cardName) {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.GrantStaticAbility(
                ConditionalStaticAbility(
                    LegendRuleDoesNotApplyTo(GameObjectFilter.Permanent),
                    Conditions.CompareAmounts(
                        DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Permanent.named(requiredName)).count(),
                        ComparisonOperator.GTE,
                        1
                    )
                ),
                EffectTarget.Controller,
                Duration.EndOfTurn
            )
        }
    }
    val echoIfNothing = conditionalEcho("Test Echo If Nothing", "Nothing Named This")
    val echoIfMountain = conditionalEcho("Test Echo If Mountain", "Mountain")

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(legendaryTwins, plainLegend, echoIfNothing, echoIfMountain))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.castAndResolve(player: EntityId, name: String) {
        giveMana(player, Color.RED, 1)
        castSpell(player, putCardInHand(player, name))
        var guard = 0
        while (guard++ < 20 && state.stack.isNotEmpty() && !isPaused) bothPass()
    }

    fun GameTestDriver.named(player: EntityId, name: String) =
        getPermanents(player).filter { getCardName(it) == name }

    test("exactly two copies on the battlefield: the condition holds and both stay") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, twinsName)
        driver.castAndResolve(you, twinsName)

        driver.pendingDecision shouldBe null
        driver.named(you, twinsName).size shouldBe 2
    }

    test("a third copy breaks 'exactly two': the legend rule sees all three and one is kept") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, twinsName)
        driver.putCreatureOnBattlefield(you, twinsName)
        driver.castAndResolve(you, twinsName)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("all three copies are candidates, not just the newest") {
            decision.options.size shouldBe 3
        }
        driver.submitCardSelection(you, listOf(decision.options.first()))
        driver.named(you, twinsName).size shouldBe 1
    }

    test("the exemption is scoped to its filter — another legend pair is still ruled") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, twinsName)
        driver.putCreatureOnBattlefield(you, twinsName)
        driver.putCreatureOnBattlefield(you, "Test Plain Legend")
        driver.castAndResolve(you, "Test Plain Legend")

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("a granted conditional exemption whose condition is false does not apply") {
        val (driver, you) = newGame()
        driver.castAndResolve(you, "Test Echo If Nothing")
        driver.putCreatureOnBattlefield(you, "Test Plain Legend")
        driver.castAndResolve(you, "Test Plain Legend")

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("a granted conditional exemption whose condition holds applies") {
        val (driver, you) = newGame()
        driver.putLandOnBattlefield(you, "Mountain")
        driver.castAndResolve(you, "Test Echo If Mountain")
        driver.putCreatureOnBattlefield(you, "Test Plain Legend")
        driver.castAndResolve(you, "Test Plain Legend")

        driver.pendingDecision shouldBe null
        driver.named(you, "Test Plain Legend").size shouldBe 2
    }
})
