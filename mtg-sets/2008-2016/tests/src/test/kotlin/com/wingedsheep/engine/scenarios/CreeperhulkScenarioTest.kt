package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c14.cards.Creeperhulk
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Creeperhulk (C14) — {1}{G}: Until end of turn, target creature you control has base power and
 * toughness 5/5 and gains trample.
 */
class CreeperhulkScenarioTest : FunSpec({

    val projector = StateProjector()

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + Creeperhulk)
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("makes a creature you control a base 5/5 with trample until end of turn") {
        val d = setup()
        val you = d.activePlayer!!
        val hulk = d.putCreatureOnBattlefield(you, "Creeperhulk")
        val lions = d.putCreatureOnBattlefield(you, "Savannah Lions")

        projector.project(d.state).hasKeyword(hulk, Keyword.TRAMPLE) shouldBe true
        projector.project(d.state).hasKeyword(lions, Keyword.TRAMPLE) shouldBe false

        d.giveMana(you, Color.GREEN, 2)
        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = hulk,
                abilityId = Creeperhulk.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(lions)),
            )
        ).error shouldBe null
        d.bothPass()

        val projected = projector.project(d.state)
        projected.getPower(lions) shouldBe 5
        projected.getToughness(lions) shouldBe 5
        projected.hasKeyword(lions, Keyword.TRAMPLE) shouldBe true

        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        val after = projector.project(d.state)
        after.getPower(lions) shouldBe 1
        after.hasKeyword(lions, Keyword.TRAMPLE) shouldBe false
    }

    test("cannot target a creature an opponent controls") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val hulk = d.putCreatureOnBattlefield(you, "Creeperhulk")
        val enemy = d.putCreatureOnBattlefield(opponent, "Savannah Lions")

        d.giveMana(you, Color.GREEN, 2)
        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = hulk,
                abilityId = Creeperhulk.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(enemy)),
            )
        ).error shouldNotBe null
    }
})
