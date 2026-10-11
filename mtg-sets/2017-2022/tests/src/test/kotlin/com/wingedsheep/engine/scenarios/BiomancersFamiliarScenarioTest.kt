package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.HydraTrainer
import com.wingedsheep.mtg.sets.definitions.rna.cards.BiomancersFamiliar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Biomancer's Familiar (RNA) — "Activated abilities of creatures you control cost {2} less to
 * activate. This effect can't reduce the mana in that cost to less than one mana." and "{T}: The
 * next time target creature adapts this turn, it adapts as though it had no +1/+1 counters on it."
 *
 * Exercised against Hydra Trainer's "{2}{G}: Adapt 2."
 */
class BiomancersFamiliarScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BiomancersFamiliar + HydraTrainer)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusOnes(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    val adapt = HydraTrainer.activatedAbilities[0].id

    test("Hydra Trainer's {2}{G} adapt costs only {G} with the Familiar out") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Biomancer's Familiar")
        val trainer = d.putCreatureOnBattlefield(me, "Hydra Trainer")

        d.giveMana(me, Color.GREEN, 1)
        d.submitSuccess(ActivateAbility(playerId = me, sourceId = trainer, abilityId = adapt))
        d.bothPass()
        d.plusOnes(trainer) shouldBe 2
    }

    test("tapping the Familiar lets a creature with +1/+1 counters adapt again, once") {
        val d = driver()
        val me = d.activePlayer!!
        val familiar = d.putCreatureOnBattlefield(me, "Biomancer's Familiar")
        val trainer = d.putCreatureOnBattlefield(me, "Hydra Trainer")
        d.removeSummoningSickness(familiar)

        d.giveMana(me, Color.GREEN, 1)
        d.submitSuccess(ActivateAbility(playerId = me, sourceId = trainer, abilityId = adapt))
        d.bothPass()
        d.plusOnes(trainer) shouldBe 2

        d.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = familiar,
                abilityId = BiomancersFamiliar.activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Permanent(trainer))
            )
        )
        d.bothPass()
        d.plusOnes(trainer) shouldBe 2

        repeat(2) {
            d.giveMana(me, Color.GREEN, 1)
            d.submitSuccess(ActivateAbility(playerId = me, sourceId = trainer, abilityId = adapt))
            d.bothPass()
        }
        d.plusOnes(trainer) shouldBe 4
    }
})
