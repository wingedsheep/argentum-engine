package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sarkhan the Masterless (WAR #143) — {3}{R}{R} planeswalker, loyalty 5.
 *
 *   Whenever a creature attacks you or a planeswalker you control, each Dragon you control deals
 *   1 damage to that creature.
 *   +1: Until end of turn, each planeswalker you control becomes a 4/4 red Dragon creature and
 *       gains flying.
 *   −3: Create a 4/4 red Dragon creature token with flying.
 *
 * Pins: the trigger fires for attacks on you *and* on your planeswalkers, and deals 1 per Dragon
 * (non-Dragons add nothing); the +1 animates every planeswalker you control, which stop being
 * planeswalkers yet can still activate their loyalty abilities.
 */
class SarkhanTheMasterlessScenarioTest : ScenarioTestBase() {

    private val sarkhanName = "Sarkhan the Masterless"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun damageOn(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("each Dragon you control deals 1 damage to each creature attacking you or your planeswalker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, sarkhanName)
                .withCardOnBattlefield(1, "Shivan Dragon")
                .withCardOnBattlefield(1, "Shivan Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val sarkhan = game.findPermanent(sarkhanName)!!
            seedLoyalty(game, sarkhan, 5)
            val giant = game.findPermanent("Hill Giant")!!
            val theirBears = game.findPermanents("Grizzly Bears")
                .single { game.state.projectedState.getController(it) == game.player2Id }

            game.execute(
                DeclareAttackers(
                    game.player2Id,
                    mapOf(giant to sarkhan, theirBears to game.player1Id),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("two Dragons deal 1 each to the Hill Giant attacking Sarkhan; the Bears add nothing") {
                damageOn(game, giant) shouldBe 2
                game.state.getBattlefield().contains(giant) shouldBe true
            }
            withClue("the Grizzly Bears attacking the player take 2 and die") {
                game.state.getBattlefield().contains(theirBears) shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }

        test("+1 turns every planeswalker you control into a 4/4 red flying Dragon that keeps its loyalty abilities") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, sarkhanName)
                .withCardOnBattlefield(1, "Lukka, Coppercoat Outcast")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sarkhan = game.findPermanent(sarkhanName)!!
            val lukka = game.findPermanent("Lukka, Coppercoat Outcast")!!
            seedLoyalty(game, sarkhan, 5)
            seedLoyalty(game, lukka, 5)
            val plusOne = cardRegistry.getCard(sarkhanName)!!.script.activatedAbilities[0]

            game.execute(ActivateAbility(game.player1Id, sarkhan, plusOne.id)).error shouldBe null
            game.resolveStack()
            loyalty(game, sarkhan) shouldBe 6

            val projected = game.state.projectedState
            for (pw in listOf(sarkhan, lukka)) {
                withClue("each planeswalker is a 4/4 red flying Dragon creature and no longer a planeswalker") {
                    projected.isCreature(pw) shouldBe true
                    projected.isPlaneswalker(pw) shouldBe false
                    projected.getPower(pw) shouldBe 4
                    projected.getToughness(pw) shouldBe 4
                    projected.hasKeyword(pw, Keyword.FLYING) shouldBe true
                    projected.hasSubtype(pw, "Dragon") shouldBe true
                    projected.getColors(pw) shouldBe setOf(Color.RED.name)
                }
            }

            withClue("Lukka's loyalty ability is still activatable while it's a Dragon creature") {
                val lukkaPlusOne = cardRegistry.getCard("Lukka, Coppercoat Outcast")!!.script.activatedAbilities[0]
                game.execute(ActivateAbility(game.player1Id, lukka, lukkaPlusOne.id)).error shouldBe null
                game.resolveStack()
                loyalty(game, lukka) shouldBe 6
            }
        }

        test("−3 creates a 4/4 red Dragon token with flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, sarkhanName)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sarkhan = game.findPermanent(sarkhanName)!!
            seedLoyalty(game, sarkhan, 5)
            val minusThree = cardRegistry.getCard(sarkhanName)!!.script.activatedAbilities[1]

            game.execute(ActivateAbility(game.player1Id, sarkhan, minusThree.id)).error shouldBe null
            game.resolveStack()

            loyalty(game, sarkhan) shouldBe 2
            val dragon = game.findPermanent("Dragon Token")!!
            val projected = game.state.projectedState
            projected.getPower(dragon) shouldBe 4
            projected.getToughness(dragon) shouldBe 4
            projected.hasKeyword(dragon, Keyword.FLYING) shouldBe true
        }
    }
}
