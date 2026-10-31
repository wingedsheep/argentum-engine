package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.CantCastFromNonHandZonesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A cast prohibition reaches the casts an effect makes while resolving — cascade (CR 702.85a),
 * discover (CR 701.57a), "cast it without paying its mana cost". Those casts skip the
 * priority-window validation (timing doesn't apply to them), and used to skip the prohibition with
 * it: an opponent's Drannith Magistrate ("Your opponents can't cast spells from anywhere other than
 * their hands") didn't stop a cascade hit being cast from exile. A forbidden cast now never
 * initiates — it isn't even offered — and the effect takes its "if you don't cast it" branch.
 */
class ResolutionCastProhibitionScenarioTest : ScenarioTestBase() {

    init {
        context("cast prohibitions on resolution-time casts") {

            test("an opponent's Drannith Magistrate stops a cascade hit being cast from exile") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Quandrix, the Proof")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Drannith Magistrate")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .build()

                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack() // Cascade exiles Grizzly Bears, then Quandrix resolves.

                withClue("A cast the Magistrate forbids is never offered") {
                    game.getPendingDecision() shouldBe null
                }

                withClue("The cascade hit can't be cast from exile under Drannith Magistrate") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Uncast, it goes to the bottom of the library with the rest") {
                    val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                    game.state.getEntity(bears)?.has<PlayWithoutPayingCostComponent>() shouldBe false
                    game.state.mayPlayPermissions.none { bears in it.cardIds } shouldBe true
                }
                withClue("Quandrix itself was cast from hand and resolves") {
                    game.isOnBattlefield("Quandrix, the Proof") shouldBe true
                }
            }

            test("the Magistrate doesn't restrict its own controller's cascade") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Quandrix, the Proof")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Drannith Magistrate")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .build()

                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()
                val yesNo = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.submitDecision(YesNoResponse(yesNo.id, choice = true)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("with no prohibition in play, a split card cascaded into is still offered") {
                // The prohibition check runs before a half is chosen, so it must not reject the
                // cast for lacking one.
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Quandrix, the Proof")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Wear // Tear")
                    .withCardInLibrary(1, "Mountain")
                    .build()

                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            }

            test("Avatar's Wrath's hand-only casting stops a cascade hit too") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Quandrix, the Proof")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .build()
                game.state = game.state.updateEntity(game.player1Id) {
                    it.with(CantCastFromNonHandZonesComponent(expiresForPlayerId = game.player2Id))
                }

                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("a discovered card the prohibition forbids casting goes to hand instead") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Trumpeting Carnosaur")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Drannith Magistrate")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .build()

                game.castSpell(1, "Trumpeting Carnosaur").error shouldBe null
                game.resolveStack() // Carnosaur's ETB discover 5 hits Grizzly Bears.

                withClue("With casting forbidden there is no cast-or-hand choice to make") {
                    game.getPendingDecision() shouldBe null
                }

                withClue("Not cast from exile under Drannith Magistrate") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Discover's fallback puts it into hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                    game.state.getEntity(bears)?.has<PlayWithoutPayingCostComponent>() shouldBe false
                }
            }
        }
    }
}
