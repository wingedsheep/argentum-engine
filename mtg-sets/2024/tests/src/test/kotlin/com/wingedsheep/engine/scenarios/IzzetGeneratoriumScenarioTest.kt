package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.IzzetGeneratorium
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Izzet Generatorium (MH3) — "If you would get one or more {E}, you get that many plus one {E}
 * instead. / {T}: Draw a card. Activate only if you've paid or lost four or more {E} this turn."
 *
 * Galvanic Discharge supplies both halves: it gets you {E}{E}{E} (four, with the Generatorium) and
 * then lets you pay any amount of it.
 */
class IzzetGeneratoriumScenarioTest : ScenarioTestBase() {

    private val drawAbility = IzzetGeneratorium.activatedAbilities.single().id

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.dischargeAndPay(pay: Int) {
        val wurm = findPermanent("Craw Wurm")!!
        castSpell(1, "Galvanic Discharge", targetId = wurm).error shouldBe null
        resolveStack()
        withClue("three {E} plus one from the Generatorium") { energy() shouldBe 4 }
        chooseNumber(pay).error shouldBe null
        resolveStack()
    }

    private fun build() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Izzet Generatorium")
        .withCardInHand(1, "Galvanic Discharge")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Izzet Generatorium") {

            test("paying four {E} unlocks the draw") {
                val game = build()
                game.dischargeAndPay(4)
                game.energy() shouldBe 0

                val handBefore = game.handSize(1)
                val generatorium = game.findPermanent("Izzet Generatorium")!!
                game.execute(ActivateAbility(game.player1Id, generatorium, drawAbility)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 1
            }

            test("paying only three {E} keeps the draw locked, even with energy left over") {
                val game = build()
                game.dischargeAndPay(3)
                game.energy() shouldBe 1

                val generatorium = game.findPermanent("Izzet Generatorium")!!
                game.execute(ActivateAbility(game.player1Id, generatorium, drawAbility)).error shouldNotBe null
            }
        }
    }
}
