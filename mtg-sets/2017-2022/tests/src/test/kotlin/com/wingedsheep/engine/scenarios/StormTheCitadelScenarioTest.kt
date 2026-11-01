package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Storm the Citadel (WAR #178, {4}{G}, Sorcery).
 *
 *   Until end of turn, creatures you control get +2/+2 and gain "Whenever this creature deals
 *   combat damage to a player or planeswalker, destroy target artifact or enchantment defending
 *   player controls."
 *
 * The granted trigger is a targeted one whose target filter reads the attacking creature's own
 * defending player — the prompt must offer that player's artifacts/enchantments and not yours.
 */
class StormTheCitadelScenarioTest : ScenarioTestBase() {

    private fun TestGame.permanentOf(playerId: EntityId, name: String): EntityId =
        state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    init {
        context("Storm the Citadel") {

            test("pumps your team and its combat damage destroys an artifact or enchantment the defending player controls") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Storm the Citadel")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val myThopter = game.permanentOf(game.player1Id, "Ornithopter")
                val theirThopter = game.permanentOf(game.player2Id, "Ornithopter")
                val anthem = game.permanentOf(game.player2Id, "Glorious Anthem")

                game.castSpell(1, "Storm the Citadel").error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Grizzly Bears is 4/4") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                }
                withClue("your Ornithopter is 2/4") {
                    projected.getPower(myThopter) shouldBe 2
                    projected.getToughness(myThopter) shouldBe 4
                }
                withClue("Bob's Ornithopter is untouched (Glorious Anthem makes it 1/3)") {
                    projected.getPower(theirThopter) shouldBe 1
                    projected.getToughness(theirThopter) shouldBe 3
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("the pumped Bears dealt 4") { game.getLifeTotal(2) shouldBe 16 }

                val decision = game.getPendingDecision()
                withClue("the granted trigger asks for a target; got $decision") {
                    decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                val legal = (decision as ChooseTargetsDecision).legalTargets[0].orEmpty()
                withClue("the defending player's artifact and enchantment are legal") {
                    legal shouldContain theirThopter
                    legal shouldContain anthem
                }
                withClue("your own artifact is not") { legal shouldNotContain myThopter }

                game.selectTargets(listOf(anthem)).error shouldBe null
                game.resolveStack()

                withClue("Glorious Anthem is destroyed") { game.isInGraveyard(2, "Glorious Anthem") shouldBe true }
                withClue("both Ornithopters survive") {
                    game.isInGraveyard(1, "Ornithopter") shouldBe false
                    game.isInGraveyard(2, "Ornithopter") shouldBe false
                }
            }
        }
    }
}
