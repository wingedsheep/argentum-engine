package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.KillingWave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Killing Wave — {X}{B} Sorcery
 * "For each creature, its controller sacrifices it unless they pay X life."
 *
 * Covers the three things that make this more than a plain edict: the choice is per creature and
 * per controller (not the caster), paying is a *cost* so a player who can't afford X is never
 * offered it, and it sweeps both sides of the board.
 */
class KillingWaveScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(KillingWave)
        return driver
    }

    test("each controller chooses per creature: pay X life to keep it, or sacrifice it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val wave = driver.putCardInHand(caster, "Killing Wave")
        driver.giveMana(caster, Color.BLACK, 3)
        driver.castXSpell(caster, wave, xValue = 2).isSuccess shouldBe true
        driver.bothPass()

        // Active player first (APNAP): the caster is asked about their own Bears.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe caster
        // Still paused afterwards (the opponent has yet to choose), so assert on `error`, not
        // `isSuccess` — the latter is false whenever a decision is still pending.
        driver.submitYesNo(caster, true).error shouldBe null

        // Then the opponent, for theirs.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent
        driver.submitYesNo(opponent, false).error shouldBe null

        driver.getLifeTotal(caster) shouldBe 18
        driver.getLifeTotal(opponent) shouldBe 20
        driver.findPermanent(caster, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Grizzly Bears")
    }

    test("each prompt names the creature it covers") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(caster, "Hill Giant")

        val wave = driver.putCardInHand(caster, "Killing Wave")
        driver.giveMana(caster, Color.BLACK, 3)
        driver.castXSpell(caster, wave, xValue = 2).isSuccess shouldBe true
        driver.bothPass()

        // Two creatures, two character-identical prompts: the only thing telling them apart is the
        // subject the gate stamps from the per-creature iteration. Order follows the loop, so
        // assert on the set rather than a specific sequence.
        val first = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().context.subjectEntityId
        driver.submitYesNo(caster, true).error shouldBe null
        val second = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().context.subjectEntityId
        driver.submitYesNo(caster, true).error shouldBe null

        setOf(first, second) shouldBe setOf(bears, giant)
        driver.getLifeTotal(caster) shouldBe 16
    }

    test("a player who cannot pay X life is never offered the choice and just sacrifices") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.setLifeTotal(opponent, 2)

        val wave = driver.putCardInHand(caster, "Killing Wave")
        driver.giveMana(caster, Color.BLACK, 6)
        driver.castXSpell(caster, wave, xValue = 5).isSuccess shouldBe true
        driver.bothPass()

        // The caster controls no creatures, and the opponent cannot afford 5 life — no prompt at all.
        driver.pendingDecision shouldBe null
        driver.getLifeTotal(opponent) shouldBe 2
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Grizzly Bears")
    }
})
