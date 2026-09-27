package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HonorWornShaku
import com.wingedsheep.mtg.sets.definitions.chk.cards.OkinaTempleToTheGrandfathers
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Honor-Worn Shaku (CHK) — "{T}: Add {C}. Tap an untapped legendary permanent you control:
 * Untap this artifact."
 */
class HonorWornShakuScenarioTest : FunSpec({

    val untapAbility = HonorWornShaku.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HonorWornShaku + OkinaTempleToTheGrandfathers)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("tapping a legendary land untaps the Shaku") {
        val d = driver()
        val me = d.player1
        val shaku = d.putPermanentOnBattlefield(me, "Honor-Worn Shaku")
        val okina = d.putLandOnBattlefield(me, "Okina, Temple to the Grandfathers")
        d.tapPermanent(shaku)

        val result = d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = shaku,
                abilityId = untapAbility,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(okina))
            )
        )
        result.outcome shouldBe Outcome.Done
        d.isTapped(okina) shouldBe true
        d.isTapped(shaku) shouldBe true // untap is on the stack
        d.bothPass()
        d.isTapped(shaku) shouldBe false
    }

    test("a nonlegendary permanent cannot pay the untap cost") {
        val d = driver()
        val me = d.player1
        val shaku = d.putPermanentOnBattlefield(me, "Honor-Worn Shaku")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.tapPermanent(shaku)

        val result = d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = shaku,
                abilityId = untapAbility,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears))
            )
        )
        result.outcome shouldNotBe Outcome.Done
        d.isTapped(bears) shouldBe false
        d.isTapped(shaku) shouldBe true
    }

    test("an opponent's legendary permanent cannot pay the untap cost") {
        val d = driver()
        val me = d.player1
        val opp = d.getOpponent(me)
        val shaku = d.putPermanentOnBattlefield(me, "Honor-Worn Shaku")
        val okina = d.putLandOnBattlefield(opp, "Okina, Temple to the Grandfathers")
        d.tapPermanent(shaku)

        val result = d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = shaku,
                abilityId = untapAbility,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(okina))
            )
        )
        result.outcome shouldNotBe Outcome.Done
        d.isTapped(okina) shouldBe false
    }
})
