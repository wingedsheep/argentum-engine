package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifySpellCostsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [ModifySpellCostsEffect]'s `duration`: an end-of-turn discount ends at the turn boundary, an
 * "until your next turn" discount keeps applying through the opponents' turns (instants cast
 * there) and ends once its controller's next turn begins. Also its `increase` and `player` axes:
 * a tax adds generic mana, and lands on the resolved player rather than the controller. And its
 * `nextSpellOnly` axis: the one-shot "the next spell you cast this turn costs {N} less", spent by the
 * first matching cast.
 */
class ModifySpellCostsDurationTest : FunSpec({

    fun instant(name: String, cost: String, script: CardScript) =
        CardDefinition.instant(name = name, manaCost = ManaCost.parse(cost), oracleText = name, script = script)

    val untilNextTurn = instant(
        "Discount Until Next Turn", "{W}",
        CardScript.spell(
            Effects.ReduceSpellCosts(GameObjectFilter.InstantOrSorcery, DynamicAmount.Fixed(1), Duration.UntilYourNextTurn)
        )
    )
    val thisTurn = instant(
        "Discount This Turn", "{W}",
        CardScript.spell(Effects.ReduceSpellCosts(GameObjectFilter.InstantOrSorcery, DynamicAmount.Fixed(1)))
    )
    val taxOpponents = instant(
        "Tax Opponents This Turn", "{W}",
        CardScript.spell(Effects.IncreaseSpellCosts(DynamicAmount.Fixed(1), player = Player.EachOpponent))
    )
    val taxMe = instant(
        "Tax Me This Turn", "{W}",
        CardScript.spell(Effects.IncreaseSpellCosts(DynamicAmount.Fixed(1)))
    )
    val nextSpell = instant(
        "Next Spell Discount", "{W}",
        CardScript.spell(Effects.ReduceNextSpellCost(DynamicAmount.Fixed(1)))
    )
    val nextCreatureSpell = instant(
        "Next Creature Spell Discount", "{W}",
        CardScript.spell(Effects.ReduceNextSpellCost(DynamicAmount.Fixed(1), GameObjectFilter.Creature))
    )
    val genericInstant = instant("One And White", "{1}{W}", CardScript.spell(Effects.GainLife(1)))
    val twoGenericInstant = instant("Two And White", "{2}{W}", CardScript.spell(Effects.GainLife(1)))
    val coloredInstant = instant("Double White", "{W}{W}", CardScript.spell(Effects.GainLife(1)))

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(
                untilNextTurn, thisTurn, taxOpponents, taxMe, nextSpell, nextCreatureSpell,
                genericInstant, twoGenericInstant, coloredInstant,
            ))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Casts [name] with exactly one white mana; returns whether the cast was legal. */
    fun castWithOneWhite(driver: GameTestDriver, player: EntityId, name: String): Boolean {
        val card = driver.putCardInHand(player, name)
        driver.giveMana(player, Color.WHITE, 1)
        val result = driver.castSpell(player, card)
        if (result.error == null) driver.bothPass()
        return result.error == null
    }

    /** Advances into the opponent's upkeep and hands [me] priority there. */
    fun toOpponentUpkeepWithPriority(driver: GameTestDriver, me: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldNotBe me
        driver.passPriority(driver.activePlayer!!)
        driver.priorityPlayer shouldBe me
    }

    test("an until-your-next-turn discount applies on the opponent's turn and ends on your next turn") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Discount Until Next Turn") shouldBe true

        withClue("same turn: {1}{W} costs {W}") {
            castWithOneWhite(driver, me, "One And White") shouldBe true
        }
        withClue("only generic mana is reduced (CR 601.2f)") {
            castWithOneWhite(driver, me, "Double White") shouldBe false
        }

        toOpponentUpkeepWithPriority(driver, me)
        withClue("the discount survives the turn boundary into the opponent's turn") {
            driver.state.spellCostModifiers.size shouldBe 1
            castWithOneWhite(driver, me, "One And White") shouldBe true
        }

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        withClue("gone once your next turn begins") {
            driver.state.spellCostModifiers shouldBe emptyList()
            castWithOneWhite(driver, me, "One And White") shouldBe false
        }
    }

    test("an opponent's untap step does not end your until-your-next-turn discount") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Discount Until Next Turn") shouldBe true
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldNotBe me
        driver.state.spellCostModifiers.single().controllerId shouldBe me
    }

    test("an end-of-turn discount ends at the turn boundary") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Discount This Turn") shouldBe true
        castWithOneWhite(driver, me, "One And White") shouldBe true

        toOpponentUpkeepWithPriority(driver, me)
        driver.state.spellCostModifiers shouldBe emptyList()
        castWithOneWhite(driver, me, "One And White") shouldBe false
    }

    test("the discount only applies to its controller's spells") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Discount Until Next Turn") shouldBe true
        toOpponentUpkeepWithPriority(driver, me)
        driver.passPriority(me)
        val opponent = driver.activePlayer!!
        castWithOneWhite(driver, opponent, "One And White") shouldBe false
    }

    test("a tax adds generic mana to its player's spells this turn") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Tax Me This Turn") shouldBe true
        withClue("{W} now costs {1}{W}") {
            castWithOneWhite(driver, me, "Discount This Turn") shouldBe false
        }
        val card = driver.putCardInHand(me, "Double White")
        driver.giveMana(me, Color.WHITE, 3)
        withClue("{W}{W} + {1} is payable with three white") {
            driver.castSpell(me, card).error shouldBe null
        }
    }

    test("a tax resolved for each opponent lands on them, not on its controller") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Tax Opponents This Turn") shouldBe true
        val opponent = driver.player2
        driver.state.spellCostModifiers.single().controllerId shouldBe opponent
        driver.state.spellCostModifiers.single().increase shouldBe true
        withClue("the controller's own spells are untaxed") {
            castWithOneWhite(driver, me, "Discount This Turn") shouldBe true
        }
    }

    test("durations other than end of turn and until your next turn are rejected") {
        shouldThrow<IllegalArgumentException> {
            ModifySpellCostsEffect(GameObjectFilter.InstantOrSorcery, DynamicAmount.Fixed(1), Duration.Permanent)
        }
    }

    test("until your next turn is rejected for anyone but you") {
        shouldThrow<IllegalArgumentException> {
            ModifySpellCostsEffect(
                GameObjectFilter.Any, DynamicAmount.Fixed(1), Duration.UntilYourNextTurn, player = Player.EachOpponent,
            )
        }
    }
    /** Casts [name] with [white] white mana and lets it resolve; returns whether the cast was legal. */
    fun castWithWhite(driver: GameTestDriver, player: EntityId, name: String, white: Int): Boolean {
        val card = driver.putCardInHand(player, name)
        driver.giveMana(player, Color.WHITE, white)
        val result = driver.castSpell(player, card)
        if (result.error == null) driver.bothPass()
        return result.error == null
    }

    test("a next-spell discount reduces only the next spell you cast") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Next Spell Discount") shouldBe true
        driver.state.spellCostModifiers.single().nextSpellOnly shouldBe true

        withClue("the next spell: {1}{W} costs {W}") {
            castWithOneWhite(driver, me, "One And White") shouldBe true
        }
        withClue("spent by that spell") {
            driver.state.spellCostModifiers shouldBe emptyList()
            castWithOneWhite(driver, me, "One And White") shouldBe false
        }
    }

    test("a next-spell discount is spent by the next spell even when it has no generic mana to reduce") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Next Spell Discount") shouldBe true
        castWithWhite(driver, me, "Double White", white = 2) shouldBe true
        driver.state.spellCostModifiers shouldBe emptyList()
    }

    test("two next-spell discounts both go to the same next spell") {
        val driver = newDriver()
        val me = driver.player1
        val first = driver.putCardInHand(me, "Next Spell Discount")
        val second = driver.putCardInHand(me, "Next Spell Discount")
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, first).error shouldBe null
        driver.castSpell(me, second).error shouldBe null
        driver.bothPass()
        driver.bothPass()
        driver.state.spellCostModifiers.size shouldBe 2

        withClue("{2}{W} costs {W}") {
            castWithOneWhite(driver, me, "Two And White") shouldBe true
        }
        driver.state.spellCostModifiers shouldBe emptyList()
    }

    test("a filtered next-spell discount waits for a matching spell") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Next Creature Spell Discount") shouldBe true
        withClue("a non-creature spell is neither discounted nor spends it") {
            castWithOneWhite(driver, me, "One And White") shouldBe false
            castWithWhite(driver, me, "One And White", white = 2) shouldBe true
            driver.state.spellCostModifiers.size shouldBe 1
        }
    }

    test("an unused next-spell discount ends with the turn") {
        val driver = newDriver()
        val me = driver.player1
        castWithOneWhite(driver, me, "Next Spell Discount") shouldBe true
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldNotBe me
        driver.state.spellCostModifiers shouldBe emptyList()
    }

    test("a next-spell discount is rejected for any duration but this turn") {
        shouldThrow<IllegalArgumentException> {
            ModifySpellCostsEffect(
                GameObjectFilter.Any, DynamicAmount.Fixed(1), Duration.UntilYourNextTurn, nextSpellOnly = true,
            )
        }
    }
})
