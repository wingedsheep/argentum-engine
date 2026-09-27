package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.CallousDeceiver
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Callous Deceiver (CHK #53) — "{1}: Look at the top card of your library. / {2}: Reveal the top
 * card of your library. If it's a land card, this creature gets +1/+0 and gains flying until end
 * of turn. Activate only once each turn."
 */
class CallousDeceiverScenarioTest : ScenarioTestBase() {

    private val lookAbility = CallousDeceiver.activatedAbilities[0].id
    private val revealAbility = CallousDeceiver.activatedAbilities[1].id

    init {
        fun base(topCard: String?) = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Callous Deceiver")
            .withLandsOnBattlefield(1, "Island", 5)
            .let { if (topCard != null) it.withCardInLibrary(1, topCard) else it }
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.activate(abilityId: com.wingedsheep.sdk.scripting.AbilityId) = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Callous Deceiver")!!,
                abilityId = abilityId,
            )
        )

        context("Callous Deceiver") {
            test("revealing a land gives +1/+0 and flying until end of turn") {
                val game = base("Forest").build()
                val deceiver = game.findPermanent("Callous Deceiver")!!

                game.activate(revealAbility).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(deceiver) shouldBe 2
                game.state.projectedState.getToughness(deceiver) shouldBe 3
                game.state.projectedState.hasKeyword(deceiver, Keyword.FLYING) shouldBe true
                withClue("the revealed card stays on top of the library") {
                    game.isInHand(1, "Forest") shouldBe false
                }
            }

            test("revealing a nonland card gives no bonus") {
                val game = base("Grizzly Bears").build()
                val deceiver = game.findPermanent("Callous Deceiver")!!

                game.activate(revealAbility).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(deceiver) shouldBe 1
                game.state.projectedState.hasKeyword(deceiver, Keyword.FLYING) shouldBe false
            }

            test("an empty library gives no bonus") {
                val game = base(null).build()
                val deceiver = game.findPermanent("Callous Deceiver")!!

                game.activate(revealAbility).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(deceiver) shouldBe 1
                game.state.projectedState.hasKeyword(deceiver, Keyword.FLYING) shouldBe false
            }

            test("the reveal ability can be activated only once each turn") {
                val game = base("Forest").build()

                game.activate(revealAbility).error shouldBe null
                game.resolveStack()
                game.activate(revealAbility).error shouldNotBe null
            }

            test("the look ability is not limited and does not grant a bonus") {
                val game = base("Forest").build()
                val deceiver = game.findPermanent("Callous Deceiver")!!

                game.activate(lookAbility).error shouldBe null
                game.resolveStack()
                game.activate(lookAbility).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(deceiver) shouldBe 1
                game.state.projectedState.hasKeyword(deceiver, Keyword.FLYING) shouldBe false
                game.isInHand(1, "Forest") shouldBe false
            }
        }
    }
}
