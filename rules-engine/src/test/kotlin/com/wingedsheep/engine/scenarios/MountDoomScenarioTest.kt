package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mount Doom — "{5}{B}{R}, {T}, Sacrifice Mount Doom and a legendary artifact: Choose up to two
 * creatures, then destroy the rest. Activate only as a sorcery."
 *
 * Exercises the Duneblast-style wrath composition (Gather all creatures → SelectFromCollection
 * ChooseUpTo(2) with storeRemainder → MoveCollection Destroy) and the dual sacrifice cost
 * (SacrificeSelf auto-paid + an explicit legendary-artifact sacrifice passed in the cost payment).
 */
class MountDoomScenarioTest : FunSpec({

    // Index 2: [0] = mana ability, [1] = ping each opponent, [2] = the wrath.
    val wrathAbilityId = TestCards.all.first { it.name == "Mount Doom" }.activatedAbilities[2].id

    test("sacrifices Mount Doom + a legendary artifact, saves chosen creatures, destroys the rest") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mountDoom = driver.putPermanentOnBattlefield(p1, "Mount Doom")
        val stone = driver.putPermanentOnBattlefield(p1, "Stone of Erech") // {1} legendary artifact
        val survivor = driver.putCreatureOnBattlefield(p1, "Grizzly Bears") // the one we'll save
        driver.putCreatureOnBattlefield(p2, "Savannah Lions")
        driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        // {5}{B}{R} = 7 mana; generic paid from black.
        driver.giveMana(p1, Color.BLACK, 6)
        driver.giveMana(p1, Color.RED, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = mountDoom,
                abilityId = wrathAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(stone)),
            )
        )
        result.isSuccess shouldBe true

        // Both halves of the cost were paid up front.
        driver.findPermanent(p1, "Mount Doom") shouldBe null
        driver.getGraveyard(p1).contains(mountDoom) shouldBe true
        driver.getGraveyard(p1).contains(stone) shouldBe true

        // Resolve: choose up to two creatures to save — keep only our Grizzly Bears.
        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(p1, CardsSelectedResponse(decision.id, listOf(survivor)))

        // The saved creature lives; the two unchosen creatures are destroyed.
        driver.getCreatures(p1).contains(survivor) shouldBe true
        driver.getCreatures(p1).size shouldBe 1
        driver.getCreatures(p2).size shouldBe 0
    }

    test("cannot activate the wrath without a legendary artifact to sacrifice") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mountDoom = driver.putPermanentOnBattlefield(p1, "Mount Doom")
        driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.BLACK, 6)
        driver.giveMana(p1, Color.RED, 1)

        // No legendary artifact is on the battlefield, so the sacrifice cost can't be paid.
        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = mountDoom,
                abilityId = wrathAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = emptyList()),
            )
        )
        result.isSuccess shouldBe false
        driver.findPermanent(p1, "Mount Doom").let { it shouldBe mountDoom }
    }
})
