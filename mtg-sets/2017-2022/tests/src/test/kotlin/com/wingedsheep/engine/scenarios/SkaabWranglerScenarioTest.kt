package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.SkaabWrangler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Skaab Wrangler. */
class SkaabWranglerScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SkaabWrangler))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, skipMulligans = true)
    }

    test("Skaab Wrangler: tapping three creatures (itself included) taps target creature") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wrangler = d.putCreatureOnBattlefield(you, "Skaab Wrangler")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(you, "Hill Giant")
        val victim = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val abilityId = SkaabWrangler.activatedAbilities.single().id

        // No `{T}` symbol in the cost: the freshly-cast Wrangler can tap itself as one of the three.
        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = wrangler,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(victim)),
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(wrangler, bear, giant))
            )
        ).outcome shouldBe Outcome.Done

        listOf(wrangler, bear, giant).forEach { d.isTapped(it) shouldBe true }
        d.isTapped(victim) shouldBe false
        d.bothPass()

        d.isTapped(victim) shouldBe true
    }
})
