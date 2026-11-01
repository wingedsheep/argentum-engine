package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rna.cards.VerityCircle
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Verity Circle (RNA #58) — {2}{U} Enchantment.
 *
 *   Whenever a creature an opponent controls becomes tapped, if it isn't being declared as an
 *   attacker, you may draw a card.
 *   {4}{U}: Tap target creature without flying.
 *
 * Pins the cause exclusion both ways (an attack declaration tap is silent, any other tap of an
 * opponent's creature triggers), the printed ruling that a vigilant attacker tapped later in
 * combat still triggers it, the "may", and the opponent scope.
 */
class VerityCircleScenarioTest : ScenarioTestBase() {

    private fun TestGame.tapWithCircle(target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Verity Circle").shouldNotBeNull(),
            abilityId = VerityCircle.activatedAbilities[0].id,
            targets = listOf(ChosenTarget.Permanent(target)),
        )
    )

    init {
        context("Verity Circle") {
            test("an opponent's creature tapped by an effect lets its controller draw") {
                val game = scenario()
                    .withPlayers("Circle", "Opponent")
                    .withCardOnBattlefield(1, "Verity Circle")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.tapWithCircle(bears).error shouldBe null
                game.resolveStack()
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

                withClue("the tap trigger offers the optional draw") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()
                game.handSize(1) shouldBe 1
            }

            test("declining the may draws nothing") {
                val game = scenario()
                    .withPlayers("Circle", "Opponent")
                    .withCardOnBattlefield(1, "Verity Circle")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.tapWithCircle(game.findPermanent("Grizzly Bears").shouldNotBeNull()).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 1
            }

            test("an opponent's creature tapped by being declared as an attacker does not trigger it") {
                val game = scenario()
                    .withPlayers("Circle", "Opponent")
                    .withCardOnBattlefield(1, "Verity Circle")
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

                withClue("the attack tap is excluded, so nothing waits on the stack or asks a question") {
                    game.state.stack.isEmpty() shouldBe true
                    game.hasPendingDecision() shouldBe false
                }
                game.handSize(1) shouldBe 0
            }

            test("a vigilant attacker tapped later in combat still triggers it (printed ruling)") {
                val game = scenario()
                    .withPlayers("Circle", "Opponent")
                    .withCardOnBattlefield(1, "Verity Circle")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Steward of Valeron")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val steward = game.findPermanent("Steward of Valeron").shouldNotBeNull()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Steward of Valeron" to 1)).error shouldBe null
                game.state.getEntity(steward)?.has<TappedComponent>() shouldBe false

                game.passPriority()
                game.tapWithCircle(steward).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()
                game.handSize(1) shouldBe 1
            }

            test("its controller's own creatures becoming tapped do not trigger it") {
                val game = scenario()
                    .withPlayers("Circle", "Opponent")
                    .withCardOnBattlefield(1, "Verity Circle")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.tapWithCircle(game.findPermanent("Grizzly Bears").shouldNotBeNull()).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe 0
            }
        }
    }
}
