package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards.ChromeCompanion
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Chrome Companion (EOE #236).
 *
 * Chrome Companion {2}
 * Artifact Creature — Dog 2/1
 * Whenever this creature becomes tapped, you gain 1 life.
 * {2}, {T}: Put target card from a graveyard on the bottom of its owner's library.
 */
class ChromeCompanionTest : FunSpec({

    val abilityId = ChromeCompanion.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("gain 1 life when activated ability taps Chrome Companion") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val companion = driver.putPermanentOnBattlefield(activePlayer, "Chrome Companion")
        driver.removeSummoningSickness(companion)

        // Put a card in the opponent's graveyard to target
        val opponent = driver.getOpponent(activePlayer)
        val targetCard = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        driver.giveColorlessMana(activePlayer, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = companion,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(targetCard, opponent, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe true

        // Stack: activated ability (bottom), BecomesTapped trigger (top)
        // Resolve the gain life trigger first
        driver.bothPass()
        driver.getLifeTotal(activePlayer) shouldBe 21

        // Resolve the activated ability: card moves to bottom of opponent's library
        driver.bothPass()
        driver.getGraveyardCardNames(opponent) shouldNotContain "Grizzly Bears"
        driver.state.getLibrary(opponent).last() shouldBe targetCard
    }

    test("gain 1 life when Chrome Companion taps to attack") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val companion = driver.putPermanentOnBattlefield(activePlayer, "Chrome Companion")
        driver.removeSummoningSickness(companion)

        driver.declareAttackers(activePlayer, listOf(companion), opponent)

        // BecomesTapped trigger fires when Chrome Companion is tapped during attack
        driver.bothPass()
        driver.getLifeTotal(activePlayer) shouldBe 21
    }

    test("activated ability puts target card on bottom of owner's library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val companion = driver.putPermanentOnBattlefield(activePlayer, "Chrome Companion")
        driver.removeSummoningSickness(companion)

        // Put a card in own graveyard
        val targetCard = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.giveColorlessMana(activePlayer, 2)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = companion,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(targetCard, activePlayer, Zone.GRAVEYARD))
            )
        )

        driver.bothPass() // gain life trigger
        driver.bothPass() // activated ability resolves

        driver.getGraveyardCardNames(activePlayer) shouldNotContain "Grizzly Bears"
        driver.state.getLibrary(activePlayer).last() shouldBe targetCard
    }

    test("cannot target cards on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val companion = driver.putPermanentOnBattlefield(activePlayer, "Chrome Companion")
        driver.removeSummoningSickness(companion)

        val battlefieldCreature = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        driver.giveColorlessMana(activePlayer, 2)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = companion,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(battlefieldCreature))
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate without paying {2}") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val companion = driver.putPermanentOnBattlefield(activePlayer, "Chrome Companion")
        driver.removeSummoningSickness(companion)

        val targetCard = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        // No mana given
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = companion,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(targetCard, opponent, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe false
    }
})
