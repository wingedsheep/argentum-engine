package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Saving Grace (HOU #21).
 *
 *   Flash. Enchant creature you control. When this Aura enters, all damage that would be dealt this
 *   turn to you and permanents you control is dealt to enchanted creature instead.
 *   Enchanted creature gets +0/+3.
 *
 * Pins: damage to you and to your other permanents moves to the enchanted creature; the opponent
 * isn't covered; and the redirect stays locked on the creature enchanted at resolution even after
 * Saving Grace leaves the battlefield (2017-07-14 ruling).
 */
class SavingGraceScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        fun builder() = scenario()
            .withPlayers("Grace", "Opponent")
            .withCardInHand(1, "Saving Grace")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(2, "Mountain", 3)
            .withLandsOnBattlefield(2, "Plains", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }

        fun TestGame.enchant(creature: EntityId) {
            castSpell(1, "Saving Grace", creature).error shouldBe null
            resolveStack()
            withClue("the enters trigger has resolved") { state.stack.isEmpty() shouldBe true }
        }

        test("enchanted creature gets +0/+3") {
            val game = builder().build()
            val giant = game.findPermanent("Hill Giant")!!
            game.enchant(giant)

            game.state.projectedState.getPower(giant) shouldBe 3
            game.state.projectedState.getToughness(giant) shouldBe 6
        }

        test("damage to you and to your other permanents is dealt to the enchanted creature") {
            val game = builder().withCardInHand(2, "Shock").withCardInHand(2, "Shock").build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.enchant(giant)

            game.passPriority()
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()
            withClue("Shock to the player went to the Giant") {
                game.getLifeTotal(1) shouldBe 20
                game.damageOn(giant) shouldBe 2
            }

            game.passPriority()
            game.castSpell(2, "Shock", bears).error shouldBe null
            game.resolveStack()
            withClue("Shock to the Bears went to the Giant") {
                game.damageOn(bears) shouldBe 0
                game.damageOn(giant) shouldBe 4
            }
        }

        test("the opponent is not covered") {
            val game = builder().withCardInHand(2, "Shock").build()
            val giant = game.findPermanent("Hill Giant")!!
            game.enchant(giant)

            game.passPriority()
            game.castSpellTargetingPlayer(2, "Shock", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.damageOn(giant) shouldBe 0
        }

        test("the redirect stays on the creature it enchanted after Saving Grace leaves") {
            val game = builder().withCardInHand(2, "Disenchant").withCardInHand(2, "Shock").build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.enchant(giant)
            val grace = game.findPermanent("Saving Grace")!!

            game.passPriority()
            game.castSpell(2, "Disenchant", grace).error shouldBe null
            game.resolveStack()
            withClue("Saving Grace is gone") { game.findPermanent("Saving Grace") shouldBe null }

            game.passPriority()
            game.castSpell(2, "Shock", bears).error shouldBe null
            game.resolveStack()

            game.damageOn(bears) shouldBe 0
            game.damageOn(giant) shouldBe 2
        }
    }
}
