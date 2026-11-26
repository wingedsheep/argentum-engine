package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flowering Lumberknot — "This creature can't attack or block unless it's paired with a creature
 * with soulbond." Exercises StatePredicate.IsPairedWith through CantAttackUnless / CantBlockUnless.
 */
class FloweringLumberknotScenarioTest : ScenarioTestBase() {

    init {
        context("Flowering Lumberknot") {

            test("unpaired, it can't attack") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Flowering Lumberknot")
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Flowering Lumberknot" to 2))
                withClue("an unpaired Lumberknot is barred from attacking") { attack.error shouldNotBe null }
            }

            test("paired with a soulbond creature, it can attack") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Flowering Lumberknot")
                    .withCardInHand(1, "Lightning Mauler")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .build()

                val knot = game.findPermanent("Flowering Lumberknot")!!
                game.castSpell(1, "Lightning Mauler").error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(knot))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Flowering Lumberknot" to 2))
                withClue("paired with Lightning Mauler, the restriction lifts: ${attack.error}") {
                    attack.error shouldBe null
                }
            }

            test("unpaired, it can't block") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Flowering Lumberknot")
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val block = game.declareBlockers(mapOf("Flowering Lumberknot" to listOf("Grizzly Bears")))
                withClue("an unpaired Lumberknot is barred from blocking") { block.error shouldNotBe null }
            }
        }
    }
}
