package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.RenownedWeaver
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Renowned Weaver (JOU #137) — {G} Creature — Human Shaman 1/1.
 *
 *   {1}{G}, Sacrifice this creature: Create a 1/3 green Spider enchantment creature token with reach.
 */
class RenownedWeaverScenarioTest : ScenarioTestBase() {

    private val abilityId = RenownedWeaver.activatedAbilities.first().id

    init {
        test("sacrificing it makes a 1/3 reach Spider that is an enchantment creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Renowned Weaver")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val weaver = game.findPermanent("Renowned Weaver")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = weaver,
                    abilityId = abilityId,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Weaver was sacrificed as part of the cost") {
                game.isInGraveyard(1, "Renowned Weaver") shouldBe true
            }
            val spider = game.findPermanent("Spider Token")!!
            val projected = game.state.projectedState
            projected.getPower(spider) shouldBe 1
            projected.getToughness(spider) shouldBe 3
            projected.hasKeyword(spider, Keyword.REACH) shouldBe true
            withClue("the Spider is an enchantment creature token") {
                projected.isCreature(spider) shouldBe true
                projected.hasType(spider, "ENCHANTMENT") shouldBe true
            }
        }
    }
}
