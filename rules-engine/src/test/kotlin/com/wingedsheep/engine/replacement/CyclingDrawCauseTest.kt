package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.DrawEvent.cyclingOf` — a draw scoped by its cause: only the draw a cycling ability
 * causes (CR 702.29a, "[Cost], Discard this card: Draw a card"), and only when the cycled card
 * matches the filter. Pinned on both sides the pattern is read: as a draw replacement (Unpredictable
 * Cyclone's shape) and as a draw trigger. The cycled card is the replacement's
 * `EffectTarget.TriggeringEntity`.
 */
class CyclingDrawCauseTest : FunSpec({

    // "If a cycling ability of a nonland card would cause you to draw a card, instead you gain life
    // equal to the cycled card's mana value plus 1." — the +1 proves the handle on the cycled card.
    val cycleSwap = card("Cycle Swap") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        replacementEffect(
            ReplaceDrawWith(
                replacementEffect = Effects.GainLife(
                    com.wingedsheep.sdk.dsl.DynamicAmounts.manaValueOf(EffectTarget.TriggeringEntity)
                ),
                appliesTo = EventPattern.DrawEvent(cyclingOf = GameObjectFilter.Nonland),
            )
        )
    }

    // "Whenever a cycling ability of a nonland card causes you to draw a card, you gain 1 life."
    val cycleWatcher = card("Cycle Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.draws(cyclingOf = GameObjectFilter.Nonland)
            effect = Effects.GainLife(1)
        }
    }

    val nonlandCycler = card("Nonland Cycler") {
        manaCost = "{3}{R}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
        keywordAbility(KeywordAbility.cycling("{1}"))
    }

    val landCycler = card("Land Cycler") {
        typeLine = "Land"
        keywordAbility(KeywordAbility.cycling("{1}"))
    }

    val plainDraw = card("Plain Draw") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }

    fun setup(vararg permanents: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(cycleSwap, cycleWatcher, nonlandCycler, landCycler, plainDraw))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        permanents.forEach { driver.putPermanentOnBattlefield(player, it) }
        return driver to player
    }

    fun cycle(driver: GameTestDriver, player: EntityId, cardName: String) {
        val cardId = driver.putCardInHand(player, cardName)
        driver.giveColorlessMana(player, 1)
        val result = driver.submit(CycleCard(player, cardId, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null) driver.bothPass()
    }

    context("as a draw replacement") {
        test("cycling a nonland card replaces the draw and the effect reads the cycled card") {
            val (driver, player) = setup("Cycle Swap")
            val handBefore = driver.getHandSize(player)
            cycle(driver, player, "Nonland Cycler")

            withClue("no card was drawn: the cycled card left and nothing replaced it") {
                driver.getHandSize(player) shouldBe handBefore
            }
            withClue("life gained equals the cycled card's mana value (4)") {
                driver.getLifeTotal(player) shouldBe 24
            }
        }

        test("cycling a land card draws normally") {
            val (driver, player) = setup("Cycle Swap")
            val handBefore = driver.getHandSize(player)
            cycle(driver, player, "Land Cycler")

            driver.getHandSize(player) shouldBe handBefore + 1
            driver.getLifeTotal(player) shouldBe 20
        }

        test("a draw no cycling ability causes is not replaced") {
            val (driver, player) = setup("Cycle Swap")
            val spell = driver.putCardInHand(player, "Plain Draw")
            val handBefore = driver.getHandSize(player)
            driver.giveColorlessMana(player, 1)
            driver.castSpell(player, spell).error shouldBe null
            while (driver.state.stack.isNotEmpty()) driver.bothPass()

            driver.getHandSize(player) shouldBe handBefore
            driver.getLifeTotal(player) shouldBe 20
        }
    }

    context("as a draw trigger") {
        test("triggers on a nonland card's cycling draw only") {
            val (driver, player) = setup("Cycle Watcher")

            cycle(driver, player, "Nonland Cycler")
            driver.getLifeTotal(player) shouldBe 21

            cycle(driver, player, "Land Cycler")
            withClue("a land card's cycling draw doesn't match the filter") {
                driver.getLifeTotal(player) shouldBe 21
            }

            val spell = driver.putCardInHand(player, "Plain Draw")
            driver.giveColorlessMana(player, 1)
            driver.castSpell(player, spell).error shouldBe null
            while (driver.state.stack.isNotEmpty()) driver.bothPass()
            withClue("a spell's draw isn't caused by cycling") {
                driver.getLifeTotal(player) shouldBe 21
            }
        }
    }
})
