package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kemba, Kha Enduring (ONE #19) {1}{W} 2/2 Legendary Cat Cleric.
 * Whenever Kemba or another Cat you control enters, attach up to one target Equipment you control
 * to that creature. Equipped creatures you control get +1/+1. {3}{W}{W}: Create a 2/2 white Cat token.
 */
class KembaKhaEnduringScenarioTest : ScenarioTestBase() {

    init {
        context("Kemba, Kha Enduring") {
            test("Kemba entering attaches the chosen Equipment to herself and the lord pumps her") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Kemba, Kha Enduring")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sword = game.findPermanent("Bonesplitter")!!
                game.castSpell(1, "Kemba, Kha Enduring").error shouldBe null
                game.resolveStack()

                withClue("ETB trigger asks for the optional Equipment target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(sword)).error shouldBe null
                game.resolveStack()

                val kemba = game.findPermanent("Kemba, Kha Enduring")!!
                game.state.getEntity(sword)!!.get<AttachedToComponent>()?.targetId shouldBe kemba
                withClue("2/2 + Bonesplitter +2/+0 + lord +1/+1 = 5/3") {
                    game.state.projectedState.getPower(kemba) shouldBe 5
                    game.state.projectedState.getToughness(kemba) shouldBe 3
                }
            }

            test("another Cat entering triggers; a non-Cat does not") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kemba, Kha Enduring")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sword = game.findPermanent("Bonesplitter")!!

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("Grizzly Bears is not a Cat — no trigger") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }

                game.castSpell(1, "Savannah Lions").error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.selectTargets(listOf(sword)).error shouldBe null
                game.resolveStack()

                val lions = game.findPermanent("Savannah Lions")!!
                game.state.getEntity(sword)!!.get<AttachedToComponent>()?.targetId shouldBe lions
                withClue("1/1 + Bonesplitter +2/+0 + lord +1/+1 = 4/2") {
                    game.state.projectedState.getPower(lions) shouldBe 4
                    game.state.projectedState.getToughness(lions) shouldBe 2
                }
                withClue("unequipped Kemba gets no bonus") {
                    game.state.projectedState.getPower(game.findPermanent("Kemba, Kha Enduring")!!) shouldBe 2
                }
            }

            test("activated ability makes a 2/2 Cat, whose entry trigger may be declined") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kemba, Kha Enduring")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kemba = game.findPermanent("Kemba, Kha Enduring")!!
                val sword = game.findPermanent("Bonesplitter")!!
                val abilityId = cardRegistry.getCard("Kemba, Kha Enduring")!!.script.activatedAbilities[0].id
                game.execute(ActivateAbility(game.player1Id, kemba, abilityId)).error shouldBe null
                game.resolveStack()

                val cat = game.findPermanent("Cat Token")
                cat shouldNotBe null
                withClue("the token's entry triggers Kemba") {
                    game.hasPendingDecision() shouldBe true
                }
                game.skipTargets().error shouldBe null
                game.resolveStack()

                game.state.getEntity(sword)!!.get<AttachedToComponent>() shouldBe null
                game.state.projectedState.getPower(cat!!) shouldBe 2
                game.state.projectedState.getToughness(cat) shouldBe 2
            }
        }
    }
}
