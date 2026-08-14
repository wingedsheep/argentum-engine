package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SupportiveParents
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Supportive Parents (SPM #119) — {2}{G} Creature — Human Citizen 3/3
 *
 *   Tap two untapped creatures you control: Add one mana of any color.
 *
 * The activation cost taps two untapped creatures you control (convoke-style tap cost,
 * `Costs.TapPermanents(2, Creature)`), and the mana ability adds one mana of any color
 * chosen by the player.
 */
class SupportiveParentsScenarioTest : FunSpec({

    val abilityId = SupportiveParents.activatedAbilities.first().id

    // A plain vanilla creature to tap for the cost.
    val bystander = CardDefinition.creature(
        name = "Test Bystander",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SupportiveParents + bystander)
        return driver
    }

    test("tap two creatures you control adds one mana of the chosen color") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val parents = driver.putPermanentOnBattlefield(p1, "Supportive Parents")
        val a = driver.putCreatureOnBattlefield(p1, "Test Bystander")
        val b = driver.putCreatureOnBattlefield(p1, "Test Bystander")

        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = parents,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(a, b))
            )
        )

        // Any-color mana prompts for a color choice — pick RED to prove "any color".
        val decision = driver.pendingDecision!!
        driver.submitDecision(p1, ColorChosenResponse(decision.id, Color.RED))

        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()
        pool!!.red shouldBe 1
        pool.total shouldBe 1

        // The two tapped creatures paid the cost.
        driver.isTapped(a) shouldBe true
        driver.isTapped(b) shouldBe true
    }

    test("cannot activate with fewer than two untapped creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val parents = driver.putPermanentOnBattlefield(p1, "Supportive Parents")
        val a = driver.putCreatureOnBattlefield(p1, "Test Bystander")

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = parents,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(a))
            )
        )
        result.isSuccess shouldBe false
    }
})
