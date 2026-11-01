package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.MirrorMarch
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Mirror March (RNA #108) — {5}{R} Enchantment
 *
 * "Whenever a nontoken creature you control enters, flip a coin until you lose a flip. For each flip
 *  you won, create a token that's a copy of that creature. Those tokens gain haste. Exile them at the
 *  beginning of the next end step."
 *
 * The flip run is mandatory — there is no "stop flipping" option — so the trigger never asks
 * anything: the number of tokens is exactly the number of flips won before the first loss.
 */
class MirrorMarchScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2
    )

    class Run(val driver: GameTestDriver, val you: EntityId, val wins: Int)

    fun tokens(driver: GameTestDriver, you: EntityId): List<EntityId> =
        driver.getPermanents(you).filter { id ->
            val entity = driver.state.getEntity(id)
            entity?.has<TokenComponent>() == true && entity.get<CardComponent>()?.name == "Test Bear"
        }

    /** Cast a Bear under Mirror March, then resolve the trigger on a seeded coin sequence. */
    fun runWithSeed(seed: Long): Run {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MirrorMarch, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Mirror March")

        val bearCard = driver.putCardInHand(you, "Test Bear")
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, bearCard)
        driver.bothPass()
        driver.state.stack.size shouldBe 1 // the Mirror March trigger

        driver.replaceState(driver.state.copy(rng = GameRng.seeded(seed)))
        val result = driver.bothPass()
        val flips = result.events.filterIsInstance<CoinFlipEvent>()
        flips.last().won shouldBe false
        return Run(driver, you, flips.count { it.won })
    }

    test("each flip won before the first loss makes one hasty copy, exiled at the next end step") {
        val run = (1L..60L).asSequence().map(::runWithSeed).first { it.wins >= 2 }
        val driver = run.driver

        // Mandatory: nothing was asked between flips.
        driver.pendingDecision.shouldBeNull()
        val copies = tokens(driver, run.you)
        copies.size shouldBe run.wins
        copies.forEach { token ->
            driver.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
            driver.state.projectedState.getPower(token) shouldBe 2
        }
        // The tokens entering don't trigger Mirror March again — they are tokens.
        driver.state.stack.shouldBeEmpty()

        driver.passPriorityUntil(Step.END)
        // One delayed exile trigger per token; resolve them all.
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        tokens(driver, run.you).shouldBeEmpty()
        driver.getPermanents(run.you).count { driver.getCardName(it) == "Test Bear" } shouldBe 1
    }

    test("losing the first flip creates no tokens and asks nothing") {
        val run = (1L..60L).asSequence().map(::runWithSeed).first { it.wins == 0 }
        withClue("no copies") { tokens(run.driver, run.you).shouldBeEmpty() }
        run.driver.pendingDecision.shouldBeNull()
        run.driver.state.stack.shouldBeEmpty()
    }
})
