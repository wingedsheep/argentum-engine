package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Rionya, Fire Dancer — {3}{R}{R} Legendary Creature — Human Wizard 3/4
 * At the beginning of combat on your turn, create X tokens that are copies of another target
 * creature you control, where X is one plus the number of instant and sorcery spells you've cast
 * this turn. They gain haste. Exile them at the beginning of the next end step.
 *
 * Covers the X count (instants and sorceries count, creature spells don't), the "another" target
 * restriction, haste on every copy, and the end-step exile that leaves the original behind.
 */
class RionyaFireDancerScenarioTest : ScenarioTestBase() {

    init {
        context("Rionya, Fire Dancer") {

            test("X is one plus the instants and sorceries cast this turn; copies have haste and are exiled at end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rionya, Fire Dancer")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withCardInHand(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rionya = game.findPermanent("Rionya, Fire Dancer")!!
                val giant = game.findPermanent("Hill Giant")!!

                // Two instant/sorcery spells and one creature spell: X = 1 + 2 = 3.
                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Volcanic Hammer", 2).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Raging Goblin").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets.getValue(0)
                legal shouldContain giant
                legal shouldNotContain rionya // "another" target creature

                game.selectTargets(listOf(giant))
                game.resolveStack()

                val tokens = game.findAllPermanents("Hill Giant").filter {
                    game.state.getEntity(it)?.has<TokenComponent>() == true
                }
                tokens shouldHaveSize 3
                val projected = game.state.projectedState
                for (token in tokens) {
                    projected.hasKeyword(token, Keyword.HASTE) shouldBe true
                    projected.getPower(token) shouldBe 3
                    projected.getToughness(token) shouldBe 3
                }
                projected.hasKeyword(giant, Keyword.HASTE) shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.findAllPermanents("Hill Giant") shouldBe listOf(giant)
            }

            test("with no instants or sorceries cast, it makes exactly one copy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rionya, Fire Dancer")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.getPendingDecision() as ChooseTargetsDecision
                game.selectTargets(listOf(giant))
                game.resolveStack()

                val tokens = game.findAllPermanents("Hill Giant").filter {
                    game.state.getEntity(it)?.has<TokenComponent>() == true
                }
                tokens shouldHaveSize 1
                game.state.projectedState.hasKeyword(tokens.single(), Keyword.HASTE) shouldBe true
            }
        }
    }
}
