package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The abilities an inline token is created with are part of its copiable values (CR 707.2): they
 * live on the token's `CardComponent` rather than as layer-six grants, so a copy of the token has
 * them, and "loses all abilities" takes them away.
 */
class TokenCopiableAbilitiesScenarioTest : ScenarioTestBase() {

    init {
        context("token-created abilities are copiable values") {

            test("a token copy of a Devil has its dies trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dance with Devils")
                    .withCardInHand(1, "Cackling Counterpart")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dance with Devils").error shouldBe null
                game.resolveStack()
                val devils = game.findPermanents("Devil Token")
                devils shouldHaveSize 2

                game.castSpell(1, "Cackling Counterpart", devils.first()).error shouldBe null
                game.resolveStack()
                val copy = game.findPermanents("Devil Token").single { it !in devils }
                withClue("The copy carries the Devil's dies trigger as copiable text") {
                    game.state.getEntity(copy)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldHaveSize 1
                }

                game.castSpell(1, "Murder", copy).error shouldBe null
                game.resolveStack()
                withClue("The copy's dies trigger fired and asks for its target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()
                withClue("The copy's dies trigger dealt 1 damage to the opponent") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("a token that loses all abilities no longer has the trigger it was created with") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dance with Devils")
                    .withCardInHand(1, "Turn to Frog")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dance with Devils").error shouldBe null
                game.resolveStack()
                val devil = game.findPermanents("Devil Token").first()

                game.castSpell(1, "Turn to Frog", devil).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Murder", devil).error shouldBe null
                game.resolveStack()

                withClue("The Frog died with no abilities, so nothing triggered") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("a token that loses all abilities can't activate the ability it was created with") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Deathpact Angel")
                    .withCardInHand(1, "Murder")
                    .withCardInHand(1, "Turn to Frog")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Deathpact Angel")!!).error shouldBe null
                game.resolveStack()
                val cleric = game.findPermanent("Cleric Token")
                cleric shouldNotBe null
                game.state = game.state.withEntity(
                    cleric!!, game.state.getEntity(cleric)!!.without<SummoningSicknessComponent>()
                )
                withClue("Before Turn to Frog, the Cleric's ability is offered") {
                    game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == cleric } shouldBe true
                }

                game.castSpell(1, "Turn to Frog", cleric).error shouldBe null
                game.resolveStack()

                withClue("A token that lost all abilities offers none") {
                    game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == cleric } shouldBe false
                }
                val abilityId = game.state.getEntity(cleric)!!.get<CardComponent>()!!.copyActivatedAbilities.single().id
                withClue("…and the handler rejects a direct activation") {
                    game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = cleric, abilityId = abilityId)
                    ).error shouldNotBe null
                }
            }
        }
    }
}
