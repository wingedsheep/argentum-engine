package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.BonebindOrator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Bonebind Orator's activated ability reads "Return another target creature card
 * from YOUR graveyard to your hand." The target restriction must reject cards in
 * an opponent's graveyard.
 */
class BonebindOratorTest : FunSpec({

    val abilityId = BonebindOrator.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("cannot target creature card in opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orator = driver.putCardInGraveyard(activePlayer, "Bonebind Orator")
        val opponentCreature = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.BLACK, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = orator,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(opponentCreature, opponent, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe false

        // Opponent's creature must still be in opponent's graveyard;
        // orator must not have been exiled (cost not paid).
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        driver.getGraveyardCardNames(activePlayer) shouldContain "Bonebind Orator"
    }

    test("cannot target itself — ability says 'another' creature card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Orator is the only creature card in the graveyard — it cannot target itself.
        val orator = driver.putCardInGraveyard(activePlayer, "Bonebind Orator")

        driver.giveMana(activePlayer, Color.BLACK, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = orator,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(orator, activePlayer, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe false

        driver.getGraveyardCardNames(activePlayer) shouldContain "Bonebind Orator"
    }

    test("enumerated legal targets exclude the orator itself and opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orator = driver.putCardInGraveyard(activePlayer, "Bonebind Orator")
        val ownCreature = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        val opponentCreature = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.BLACK, 4)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, activePlayer)

        val oratorActivation = actions.firstOrNull {
            val a = it.action
            a is ActivateAbility && a.sourceId == orator && a.abilityId == abilityId
        }
        oratorActivation.shouldNotBeNull()

        val validTargets = oratorActivation.validTargets ?: emptyList()
        validTargets shouldContain ownCreature
        validTargets shouldNotContain orator
        validTargets shouldNotContain opponentCreature
    }

    test("can target another creature card in your own graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orator = driver.putCardInGraveyard(activePlayer, "Bonebind Orator")
        val ownCreature = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.BLACK, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = orator,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(ownCreature, activePlayer, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        driver.getHand(activePlayer) shouldContain ownCreature
        driver.getGraveyardCardNames(activePlayer) shouldNotContain "Grizzly Bears"
    }
})
