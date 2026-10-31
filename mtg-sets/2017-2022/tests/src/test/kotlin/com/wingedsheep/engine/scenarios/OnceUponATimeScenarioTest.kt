package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.OnceUponATime
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Once Upon a Time — "If this spell is the first spell you've cast this game, you may cast it
 * without paying its mana cost. Look at the top five cards of your library. You may reveal a
 * creature or land card from among them and put it into your hand. Put the rest on the bottom of
 * your library in a random order."
 *
 * Pins the free-cast gate: offered while you've cast nothing this game, gone (and refused on
 * authorization) once you have, and blind to the opponent's spells. The counter's own rules
 * (countered spells count, it never resets) live in `SpellsCastThisGameTest`.
 */
class OnceUponATimeScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OnceUponATime))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    fun freeCast(you: EntityId, card: EntityId) = CastSpell(
        playerId = you,
        cardId = card,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
    )

    fun GameTestDriver.freeCastOffered(you: EntityId, card: EntityId): Boolean =
        legalActions(you).any { legal: LegalAction ->
            val action = legal.action
            action is CastSpell && action.cardId == card && action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    fun GameTestDriver.resolveTakingFirst() {
        var guard = 0
        while (guard++ < 20) {
            val pending = state.pendingDecision
            when {
                pending is SelectCardsDecision -> submitCardSelection(pending.playerId, pending.options.take(1))
                stackSize > 0 -> bothPass()
                else -> return
            }
        }
    }

    test("as your first spell this game it is cast for free and puts a land into your hand") {
        val (driver, you) = setup()
        val once = driver.putCardInHand(you, "Once Upon a Time")
        val handBefore = driver.getHand(you).size

        withClue("the free cast is offered with no mana available") { driver.freeCastOffered(you, once) shouldBe true }
        driver.submit(freeCast(you, once)).error shouldBe null
        driver.resolveTakingFirst()

        withClue("the spell left the hand and a Forest from the top five joined it") {
            driver.getHand(you).size shouldBe handBefore
            driver.getHand(you).count { driver.getCardName(it) == "Forest" } shouldBe handBefore
        }
        driver.getGraveyardCardNames(you).contains("Once Upon a Time") shouldBe true
    }

    test("once you've cast another spell this game the free cast is gone, but {1}{G} still works") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(opponent)).error shouldBe null
        driver.bothPass()

        val once = driver.putCardInHand(you, "Once Upon a Time")
        driver.freeCastOffered(you, once) shouldBe false
        withClue("authorization refuses it too, not just the enumerator") {
            driver.submit(freeCast(you, once)).error shouldBe
                "Alternative cost is not available: ${OnceUponATime.script.selfAlternativeCost!!.condition!!.description}"
        }

        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, once).error shouldBe null
    }

    test("the opponent's spells don't use up your first spell") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(you)
        driver.castSpell(opponent, bolt, listOf(you)).error shouldBe null
        driver.bothPass()
        withClue("the opponent's Bolt resolved") { driver.getLifeTotal(you) shouldBe 17 }

        val once = driver.putCardInHand(you, "Once Upon a Time")
        driver.freeCastOffered(you, once) shouldBe true
    }
})
