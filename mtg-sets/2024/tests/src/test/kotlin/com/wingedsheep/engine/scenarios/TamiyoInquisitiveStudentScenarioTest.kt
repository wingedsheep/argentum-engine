package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tamiyo, Inquisitive Student // Tamiyo, Seasoned Scholar (MH3 #242).
 *
 * Front: flying; attacking investigates; drawing your third card in a turn exiles Tamiyo and
 * returns her transformed.
 * Back: +2 until your next turn, creatures attacking you or your planeswalkers get -1/-0;
 * −3 regrow an instant or sorcery (green → add one mana of any color); −7 draw half your library
 * rounded up and get a no-maximum-hand-size emblem.
 */
class TamiyoInquisitiveStudentScenarioTest : ScenarioTestBase() {

    private val front = "Tamiyo, Inquisitive Student"
    private val back = "Tamiyo, Seasoned Scholar"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun abilityId(index: Int) = cardRegistry.requireCard(back).script.activatedAbilities[index].id

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun scholarOnMyTurn(extra: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, back)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(extra)
        .build()

    init {
        test("attacking with Tamiyo investigates") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(front to 2)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Clue") shouldNotBe null
        }

        test("drawing your third card in a turn flips Tamiyo") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardsDrawnThisTurn(1, 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            game.findPermanent(front) shouldBe null
            val scholar = game.findPermanent(back)
            withClue("Tamiyo returned transformed") { scholar shouldNotBe null }
            loyalty(game, scholar!!) shouldBe 2
        }

        test("drawing only a second card in a turn doesn't flip Tamiyo") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            game.findPermanent(front) shouldNotBe null
            game.findPermanent(back) shouldBe null
        }

        test("+2: until your next turn, a creature attacking you gets -1/-0") {
            val game = scholarOnMyTurn {
                withCardOnBattlefield(2, "Grizzly Bears")
            }
            val tamiyo = game.findPermanent(back)!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(ActivateAbility(game.player1Id, tamiyo, abilityId(0))).error shouldBe null
            game.resolveStack()
            loyalty(game, tamiyo) shouldBe 4

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()

            withClue("the attacking Bears get -1/-0") {
                game.state.projectedState.getPower(bears) shouldBe 1
                game.state.projectedState.getToughness(bears) shouldBe 2
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("the -1/-0 lasted only until end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 2
            }
        }

        test("−3 returns a non-green instant without adding mana") {
            val game = scholarOnMyTurn {
                withCardInGraveyard(1, "Think Twice")
            }
            val tamiyo = game.findPermanent(back)!!
            setLoyalty(game, tamiyo, 4)
            val spell = game.findCardsInGraveyard(1, "Think Twice").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, tamiyo, abilityId(1),
                    targets = listOf(ChosenTarget.Card(spell, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Think Twice") shouldBe true
            game.getPendingDecision() shouldBe null
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
            (pool?.let { it.white + it.blue + it.black + it.red + it.green + it.colorless } ?: 0) shouldBe 0
        }

        test("−3 on a green instant also adds one mana of any color") {
            val game = scholarOnMyTurn {
                withCardInGraveyard(1, "Giant Growth")
            }
            val tamiyo = game.findPermanent(back)!!
            setLoyalty(game, tamiyo, 4)
            val spell = game.findCardsInGraveyard(1, "Giant Growth").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, tamiyo, abilityId(1),
                    targets = listOf(ChosenTarget.Card(spell, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)),
                )
            ).error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            if (decision is ChooseColorDecision) {
                game.submitDecision(ColorChosenResponse(decision.id, Color.RED)).error shouldBe null
            }

            game.isInHand(1, "Giant Growth") shouldBe true
            loyalty(game, tamiyo) shouldBe 1
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            withClue("one mana of the chosen color") { pool.red shouldBe 1 }
        }

        test("−7 draws half the library rounded up") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, back)
                .apply { repeat(5) { withCardInLibrary(1, "Island") } }
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tamiyo = game.findPermanent(back)!!
            setLoyalty(game, tamiyo, 8)
            val handBefore = game.handSize(1)

            game.execute(ActivateAbility(game.player1Id, tamiyo, abilityId(2))).error shouldBe null
            game.resolveStack()

            withClue("5 cards in library → draw 3") { game.handSize(1) shouldBe handBefore + 3 }
            game.librarySize(1) shouldBe 2
            loyalty(game, tamiyo) shouldBe 1
            withClue("the emblem removes the maximum hand size") {
                game.state.getEntity(game.player1Id)?.has<PlayerNoMaximumHandSizeComponent>() shouldBe true
            }
        }
    }
}
