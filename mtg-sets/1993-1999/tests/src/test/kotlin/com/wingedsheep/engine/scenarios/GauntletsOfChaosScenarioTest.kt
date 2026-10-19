package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.leg.cards.GauntletsOfChaos
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gauntlets of Chaos (LEG #278) — "{5}, Sacrifice this artifact: Exchange control of target
 * artifact, creature, or land you control and target permanent an opponent controls that shares one
 * of those types with it. If those permanents are exchanged this way, destroy all Auras attached to
 * them."
 */
class GauntletsOfChaosScenarioTest : ScenarioTestBase() {

    private val abilityId = GauntletsOfChaos.activatedAbilities.first().id

    init {
        context("Gauntlets of Chaos") {

            fun builder() = scenario()
                .withPlayers("Gauntlets Player", "Opponent")
                .withCardOnBattlefield(1, "Gauntlets of Chaos")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                .withCardOnBattlefield(1, "Eidolon of Rhetoric")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Crusade")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .also { b ->
                    repeat(5) {
                        b.withCardInLibrary(1, "Plains")
                        b.withCardInLibrary(2, "Mountain")
                    }
                }

            fun TestGame.activate(yours: String, theirs: String) = execute(
                ActivateAbility(
                    player1Id, findPermanent("Gauntlets of Chaos")!!, abilityId,
                    listOf(ChosenTarget.Permanent(findPermanent(yours)!!), ChosenTarget.Permanent(findPermanent(theirs)!!))
                )
            )

            fun TestGame.controllerOf(name: String) = state.projectedState.getController(findPermanent(name)!!)

            test("exchanges two creatures and destroys the Auras attached to them") {
                val game = builder()
                    .withCardAttachedTo(2, "Holy Strength", "Hill Giant")
                    .build()

                game.activate("Grizzly Bears", "Hill Giant").error shouldBe null
                game.resolveStack()

                game.controllerOf("Grizzly Bears") shouldBe game.player2Id
                game.controllerOf("Hill Giant") shouldBe game.player1Id
                withClue("both Holy Strengths are destroyed") {
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                    game.isInGraveyard(2, "Holy Strength") shouldBe true
                }
                withClue("the Gauntlets were sacrificed to pay") {
                    game.isInGraveyard(1, "Gauntlets of Chaos") shouldBe true
                }
            }

            test("the second target must share artifact, creature, or land with the first") {
                val game = builder().build()

                withClue("a creature and a land share none of those types") {
                    game.activate("Grizzly Bears", "Mountain").error shouldNotBe null
                }
                withClue("an enchantment creature and an enchantment share only enchantment") {
                    game.activate("Eidolon of Rhetoric", "Crusade").error shouldNotBe null
                }
                withClue("the first target must be yours") {
                    game.activate("Hill Giant", "Grizzly Bears").error shouldNotBe null
                }
                game.isOnBattlefield("Gauntlets of Chaos") shouldBe true

                game.activate("Eidolon of Rhetoric", "Hill Giant").error shouldBe null
                game.resolveStack()
                game.controllerOf("Eidolon of Rhetoric") shouldBe game.player2Id
                game.controllerOf("Hill Giant") shouldBe game.player1Id
            }

            test("if a target is illegal on resolution, nothing is exchanged and no Aura is destroyed") {
                val game = builder()
                    .withCardInHand(2, "Lightning Bolt")
                    .build()

                game.activate("Grizzly Bears", "Hill Giant").error shouldBe null
                game.passPriority()
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.controllerOf("Grizzly Bears") shouldBe game.player1Id
                withClue("the exchange didn't happen, so the Bears keep their Aura") {
                    game.isOnBattlefield("Holy Strength") shouldBe true
                }
            }
        }
    }
}
