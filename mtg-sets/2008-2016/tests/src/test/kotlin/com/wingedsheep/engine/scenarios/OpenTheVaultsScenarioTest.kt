package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Open the Vaults (M10 #21) — {4}{W}{W} Sorcery.
 *
 * "Return all artifact and enchantment cards from all graveyards to the battlefield under their
 *  owners' control. (Auras with nothing to enchant remain in graveyards.)"
 *
 * The Aura clause is what makes this card more than a filtered mass reanimation: the returning
 * Aura's owner picks its host (CR 303.4f), never a card entering alongside it, and an Aura with
 * no legal host stays in the graveyard.
 */
class OpenTheVaultsScenarioTest : ScenarioTestBase() {

    init {
        context("Open the Vaults") {

            test("returns every player's artifacts and enchantments to their owners; the Aura's owner picks a host already on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Open the Vaults")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInGraveyard(1, "Pacifism")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardInGraveyard(2, "Glorious Anthem")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Open the Vaults").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.playerId shouldBe game.player1Id

                val courser = game.findPermanent("Centaur Courser")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Ornithopter is entering alongside Pacifism, so it can't be enchanted") {
                    decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(courser, bears)
                }

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                val pacifism = game.findPermanent("Pacifism")!!
                game.state.getEntity(pacifism)!!.get<AttachedToComponent>()!!.targetId shouldBe bears
                game.state.getEntity(pacifism)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id

                val ornithopter = game.findPermanent("Ornithopter")!!
                game.state.getEntity(ornithopter)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
                val anthem = game.findPermanent("Glorious Anthem")!!
                game.state.getEntity(anthem)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id

                withClue("creature cards aren't artifacts or enchantments, so they stay put") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                game.isInGraveyard(1, "Pacifism") shouldBe false
                game.isInGraveyard(2, "Glorious Anthem") shouldBe false
            }

            test("an Aura with nothing to enchant stays in the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Open the Vaults")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInGraveyard(1, "Pacifism")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Open the Vaults").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isOnBattlefield("Ornithopter") shouldBe true
                game.isOnBattlefield("Pacifism") shouldBe false
                game.isInGraveyard(1, "Pacifism") shouldBe true
            }
        }
    }
}
