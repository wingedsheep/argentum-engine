package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deathpact Angel (GTC #153): when it dies, create a 1/1 white and black Cleric token with
 * "{3}{W}{B}{B}, {T}, Sacrifice this token: Return a card named Deathpact Angel from your
 * graveyard to the battlefield."
 *
 * The Cleric's ability is one of the token's copiable values (CR 707.2), so a token copy of the
 * Cleric (Cackling Counterpart) has it too.
 */
class DeathpactAngelScenarioTest : ScenarioTestBase() {

    private fun TestGame.unsick(id: EntityId) {
        state = state.withEntity(id, state.getEntity(id)!!.without<SummoningSicknessComponent>())
    }

    private fun TestGame.clericAbility(cleric: EntityId) =
        state.getEntity(cleric)!!.get<CardComponent>()!!.copyActivatedAbilities.single().id

    private fun TestGame.activateCleric(cleric: EntityId) = execute(
        ActivateAbility(playerId = player1Id, sourceId = cleric, abilityId = clericAbility(cleric))
    )

    private fun TestGame.resolveChoosingAll() {
        resolveStack()
        while (hasPendingDecision()) {
            selectCards(findCardsInGraveyard(1, "Deathpact Angel").take(1))
            resolveStack()
        }
    }

    init {
        context("Deathpact Angel") {

            test("dies into a Cleric that sacrifices itself to return the Angel") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Deathpact Angel")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Deathpact Angel")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Deathpact Angel") shouldBe true
                val cleric = game.findPermanent("Cleric Token")
                withClue("The Angel's death created the Cleric token") { cleric shouldNotBe null }
                cleric!!
                game.state.projectedState.getPower(cleric) shouldBe 1
                game.state.projectedState.getToughness(cleric) shouldBe 1

                game.unsick(cleric)
                withClue("Activating the Cleric's ability") { game.activateCleric(cleric).error shouldBe null }
                withClue("The token was sacrificed as a cost") { game.findPermanent("Cleric Token") shouldBe null }
                game.resolveChoosingAll()

                withClue("Deathpact Angel returned from the graveyard to the battlefield") {
                    game.findPermanent("Deathpact Angel") shouldNotBe null
                    game.isInGraveyard(1, "Deathpact Angel") shouldBe false
                }
            }

            test("the ability resolves to nothing when no Deathpact Angel is in the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Deathpact Angel")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Deathpact Angel")!!).error shouldBe null
                game.resolveStack()
                val cleric = game.findPermanent("Cleric Token")!!
                // Move the Angel out of the graveyard so there's nothing to return.
                val angelCard = game.findCardsInGraveyard(1, "Deathpact Angel").single()
                game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.GRAVEYARD), angelCard)

                game.unsick(cleric)
                game.activateCleric(cleric).error shouldBe null
                game.resolveChoosingAll()

                game.findPermanent("Deathpact Angel") shouldBe null
                game.findPermanent("Cleric Token") shouldBe null
            }

            test("a token copy of the Cleric has the Cleric's ability (copiable values)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Deathpact Angel")
                    .withCardInHand(1, "Murder")
                    .withCardInHand(1, "Cackling Counterpart")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Deathpact Angel")!!).error shouldBe null
                game.resolveStack()
                val original = game.findPermanent("Cleric Token")!!

                game.castSpell(1, "Cackling Counterpart", original).error shouldBe null
                game.resolveStack()

                val clerics = game.findPermanents("Cleric Token")
                clerics shouldHaveSize 2
                val copy = clerics.single { it != original }
                withClue("The copy carries the Cleric's activated ability as copiable text") {
                    game.state.getEntity(copy)!!.get<CardComponent>()!!.copyActivatedAbilities shouldHaveSize 1
                }

                game.unsick(copy)
                withClue("The copy's ability is offered as a legal action") {
                    game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == copy } shouldBe true
                }
                withClue("Activating the copy's ability") { game.activateCleric(copy).error shouldBe null }
                game.resolveChoosingAll()

                withClue("The copy's ability returned the Angel") {
                    game.findPermanent("Deathpact Angel") shouldNotBe null
                }
                withClue("The original Cleric is untouched") { game.findPermanent("Cleric Token") shouldBe original }
            }
        }
    }
}
