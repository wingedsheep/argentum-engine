package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Dispersing Orb.
 *
 * Dispersing Orb
 * {3}{U}{U}
 * Enchantment
 * {3}{U}, Sacrifice a permanent: Return target permanent to its owner's hand.
 */
class DispersingOrbTest : FunSpec({

    val DispersingOrb = card("Dispersing Orb") {
        manaCost = "{3}{U}{U}"
        typeLine = "Enchantment"
        oracleText = "{3}{U}, Sacrifice a permanent: Return target permanent to its owner's hand."

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{3}{U}"),
                Costs.Sacrifice(GameObjectFilter.Permanent)
            )
            target = Targets.Permanent
            effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
        }
    }

    val abilityId = DispersingOrb.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DispersingOrb))
        return driver
    }

    test("sacrifice a permanent to bounce target permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orb = driver.putPermanentOnBattlefield(activePlayer, "Dispersing Orb")
        val sacTarget = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val bounceTarget = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Give mana: {3}{U}
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = orb,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bounceTarget)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacTarget))
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Sacrificed creature should be gone from battlefield
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null

        // Bounced creature should be gone from opponent's battlefield
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null

        // Dispersing Orb should remain on the battlefield
        driver.findPermanent(activePlayer, "Dispersing Orb") shouldNotBe null
    }

    test("can sacrifice any permanent including lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orb = driver.putPermanentOnBattlefield(activePlayer, "Dispersing Orb")
        val land = driver.putPermanentOnBattlefield(activePlayer, "Island")
        val bounceTarget = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.giveMana(activePlayer, Color.BLUE, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = orb,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bounceTarget)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(land))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Bounced creature should be gone
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }

    test("can bounce own permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orb = driver.putPermanentOnBattlefield(activePlayer, "Dispersing Orb")
        val sacTarget = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val bounceTarget = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        driver.giveMana(activePlayer, Color.BLUE, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = orb,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bounceTarget)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacTarget))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Bounced creature should be back in hand
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe null
    }
})
