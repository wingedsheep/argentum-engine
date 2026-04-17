package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.BarkKnuckleBoxer
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.IntrepidRabbit
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.LongstalkBrawl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Longstalk Brawl - a Gift a tapped Fish sorcery from Bloomburrow.
 *
 * Mode 0: No gift — your creature fights opponent's creature.
 * Mode 1: Gift — opponent creates a tapped 1/1 blue Fish token,
 *         +1/+1 counter on your creature, then your creature fights opponent's creature.
 */
class LongstalkBrawlTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LongstalkBrawl)
        driver.registerCard(BarkKnuckleBoxer)
        driver.registerCard(IntrepidRabbit)
        return driver
    }

    test("mode 1 (gift) creates Fish token, places +1/+1 counter, then resolves fight (cast-time mode)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val yours = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.giveMana(activePlayer, Color.GREEN, 1)
        val spell = driver.putCardInHand(activePlayer, "Longstalk Brawl")

        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = spell,
            targets = listOf(
                ChosenTarget.Permanent(yours),
                ChosenTarget.Permanent(theirs)
            ),
            chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(
                ChosenTarget.Permanent(yours),
                ChosenTarget.Permanent(theirs)
            ))
        ))
        result.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(opponent, "Fish Token").shouldNotBeNull()
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe yours

        val yourCounters = driver.state.getEntity(yours)?.get<CountersComponent>()
        (yourCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1

        (theirs in driver.state.getZone(opponent, Zone.GRAVEYARD)) shouldBe true

        val yourDamage = driver.state.getEntity(yours)?.get<DamageComponent>()?.amount ?: 0
        yourDamage shouldBe 1
    }

    test("mode 1 (gift) cast with chosenModes but EMPTY modeTargetsOrdered (UI flow)") {
        // The web client sends CastSpell with chosenModes=[1] and a flat targets=[a, b],
        // but does NOT populate modeTargetsOrdered. This test mirrors that path.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val yours = driver.putCreatureOnBattlefield(activePlayer, "Bark-Knuckle Boxer")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Intrepid Rabbit")

        driver.giveMana(activePlayer, Color.GREEN, 1)
        val spell = driver.putCardInHand(activePlayer, "Longstalk Brawl")

        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = spell,
            targets = listOf(
                ChosenTarget.Permanent(yours),
                ChosenTarget.Permanent(theirs)
            ),
            chosenModes = listOf(1)
            // NOTE: modeTargetsOrdered is intentionally omitted (client doesn't populate it).
        ))
        result.isSuccess shouldBe true

        driver.bothPass()

        // Gift: opponent has a Fish Token.
        driver.findPermanent(opponent, "Fish Token").shouldNotBeNull()

        // Fight resolved: both 3/2 (with Boxer at 4/3 from counter) trade.
        (yours in driver.state.getZone(activePlayer, Zone.GRAVEYARD)) shouldBe true
        (theirs in driver.state.getZone(opponent, Zone.GRAVEYARD)) shouldBe true
    }

    test("mode 1 (gift) creates Fish token, places +1/+1 counter, then resolves fight (resolution-time mode)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val yours = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.giveMana(activePlayer, Color.GREEN, 1)
        val spell = driver.putCardInHand(activePlayer, "Longstalk Brawl")

        // Cast WITHOUT pre-choosing the mode — legacy resolution-time path.
        driver.submit(CastSpell(playerId = activePlayer, cardId = spell)).isSuccess shouldBe true

        // Pass priority to resolve the spell — this triggers the mode selection prompt.
        driver.bothPass()

        // Mode selection
        val modeDecision = driver.pendingDecision
        modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submit(SubmitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 1)))

        // Target selection
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitMultiTargetSelection(activePlayer, mapOf(0 to listOf(yours), 1 to listOf(theirs)))

        // Drain any remaining priority to finish resolution.
        if (driver.isPaused.not()) driver.bothPass()

        driver.findPermanent(opponent, "Fish Token").shouldNotBeNull()
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe yours

        val yourCounters = driver.state.getEntity(yours)?.get<CountersComponent>()
        (yourCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1

        (theirs in driver.state.getZone(opponent, Zone.GRAVEYARD)) shouldBe true

        val yourDamage = driver.state.getEntity(yours)?.get<DamageComponent>()?.amount ?: 0
        yourDamage shouldBe 1
    }
})
