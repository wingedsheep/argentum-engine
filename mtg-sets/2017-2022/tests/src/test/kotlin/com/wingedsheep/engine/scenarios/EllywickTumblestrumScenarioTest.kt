package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.EllywickTumblestrum
import com.wingedsheep.mtg.sets.definitions.chk.cards.IsamaruHoundOfKonda
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dungeons.Dungeons
import com.wingedsheep.sdk.model.DungeonDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ellywick Tumblestrum (AFR #181, {2}{G}{G}, loyalty 4).
 *
 * The −2 gains 3 life only when the card it put into hand is legendary. The −7 emblem's
 * "+2/+2 for each differently named dungeon you've completed" counts each dungeon name once and is
 * re-read as dungeons are completed after the emblem exists.
 */
class EllywickTumblestrumScenarioTest : ScenarioTestBase() {

    private val name = "Ellywick Tumblestrum"

    init {
        cardRegistry.register(listOf(EllywickTumblestrum, IsamaruHoundOfKonda))

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        /** Top six: the legendary Isamaru, a nonlegendary Grizzly Bears and four Forests; a Plains below them. */
        fun lookBoard() = board().apply {
            withCardInLibrary(1, "Isamaru, Hound of Konda")
            withCardInLibrary(1, "Grizzly Bears")
            repeat(4) { withCardInLibrary(1, "Forest") }
            withCardInLibrary(1, "Plains")
        }

        test("+1 ventures into the dungeon") {
            val game = board().build()
            val ellywick = game.findPermanent(name)!!
            setLoyalty(game, ellywick, 4)

            activate(game, ellywick, index = 0)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                .options shouldContain "Lost Mine of Phandelver"
            loyalty(game, ellywick) shouldBe 5
        }

        test("−2 taking a legendary creature gains 3 life; the rest go to the bottom") {
            val game = lookBoard().build()
            val ellywick = game.findPermanent(name)!!
            setLoyalty(game, ellywick, 4)

            activate(game, ellywick, index = 1)
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val isamaru = decision.options.single { game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Isamaru, Hound of Konda" }
            game.selectCards(listOf(isamaru)).error shouldBe null
            game.resolveStack()

            game.state.getHand(game.player1Id) shouldContain isamaru
            withClue("Isamaru is legendary: +3 life") { game.getLifeTotal(1) shouldBe 23 }
            val library = game.state.getLibrary(game.player1Id)
            library.size shouldBe 6
            withClue("the five unchosen cards went under the Plains, which is now on top") {
                game.state.getEntity(library.first())!!.get<CardComponent>()!!.name shouldBe "Plains"
            }
            loyalty(game, ellywick) shouldBe 2
        }

        test("−2 taking a nonlegendary creature gains no life") {
            val game = lookBoard().build()
            val ellywick = game.findPermanent(name)!!
            setLoyalty(game, ellywick, 4)

            activate(game, ellywick, index = 1)
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val bears = decision.options.single { game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Grizzly Bears" }
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.getHand(game.player1Id) shouldContain bears
            game.getLifeTotal(1) shouldBe 20
        }

        test("−7 emblem: trample, haste and +2/+2 per differently named dungeon, re-read as more are completed") {
            val game = board().build()
            val ellywick = game.findPermanent(name)!!
            val bears = game.state.getBattlefield().filter {
                game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Grizzly Bears"
            }
            val mine = bears.single { game.state.projectedState.getController(it) == game.player1Id }
            val theirs = bears.single { it != mine }

            game.complete(Dungeons.LostMineOfPhandelver, room = 6)
            game.complete(Dungeons.LostMineOfPhandelver, room = 6)
            game.complete(Dungeons.TombOfAnnihilation, room = 4)

            setLoyalty(game, ellywick, 7)
            activate(game, ellywick, index = 2)
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Lost Mine twice + Tomb once = two names: +4/+4") {
                projected.getPower(mine) shouldBe 6
                projected.getToughness(mine) shouldBe 6
            }
            projected.getKeywords(mine) shouldContainAll listOf(Keyword.TRAMPLE.name, Keyword.HASTE.name)
            withClue("the opponent's creature is untouched") {
                projected.getPower(theirs) shouldBe 2
                projected.getKeywords(theirs) shouldNotContain Keyword.TRAMPLE.name
            }

            game.complete(Dungeons.DungeonOfTheMadMage, room = 0)
            withClue("a third name after the emblem exists: +6/+6") {
                game.state.projectedState.getPower(mine) shouldBe 8
            }
            game.complete(Dungeons.TombOfAnnihilation, room = 4)
            withClue("Tomb again is not a new name") {
                game.state.projectedState.getPower(mine) shouldBe 8
            }
        }
    }

    /** Put [dungeon] in the command zone at [room] and complete it, without triggering anything. */
    private fun TestGame.complete(dungeon: DungeonDefinition, room: Int) {
        var s = DungeonService.enter(state, player1Id, dungeon, "test").first
        s = DungeonService.moveTo(s, player1Id, room, "test").first
        state = DungeonService.complete(s, player1Id).first
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id)
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
