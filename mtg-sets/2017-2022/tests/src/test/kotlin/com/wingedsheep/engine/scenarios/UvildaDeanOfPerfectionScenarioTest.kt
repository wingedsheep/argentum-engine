package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.stx.cards.UvildaDeanOfPerfection
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Uvilda, Dean of Perfection // Nassari, Dean of Expression (STX #160).
 *
 * Uvilda's {T} exiles an instant or sorcery with three refine counters, and the card gains an
 * upkeep countdown plus a "when the last is removed, you may cast it for {4} less" trigger. The
 * rulings pin the reduction: generic only, applied after cost increases. Nassari grows whenever
 * you cast a spell from exile.
 */
class UvildaDeanOfPerfectionScenarioTest : ScenarioTestBase() {

    /** {4}{U}: reduced by four it costs {U}. */
    private val insight = card("Uvilda Test Insight") {
        manaCost = "{4}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(5) }
    }

    /** {2}{U}{U}: the reduction never touches the blue pips, so one Island can't pay it. */
    private val doubleBlue = card("Uvilda Test Tide") {
        manaCost = "{2}{U}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(5) }
    }

    /** {2}{U}: with Thalia's {1} tax, increases come first — {3}{U} less four is {U}. */
    private val cheap = card("Uvilda Test Spark") {
        manaCost = "{2}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(5) }
    }

    /** {6}{U}: only four of the six generic come off, so it still costs {2}{U}. */
    private val big = card("Uvilda Test Torrent") {
        manaCost = "{6}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(5) }
    }

    init {
        listOf(insight, doubleBlue, cheap, big).forEach { cardRegistry.register(it) }

        fun TestGame.exiled(name: String): EntityId? =
            state.getExile(player1Id).firstOrNull { state.getEntity(it)!!.get<CardComponent>()!!.name == name }

        fun TestGame.refine(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.REFINE) ?: 0

        fun TestGame.exileWithUvilda(name: String) {
            val uvilda = findPermanent("Uvilda, Dean of Perfection")!!
            val abilityId = UvildaDeanOfPerfection.activatedAbilities[0].id
            execute(ActivateAbility(player1Id, uvilda, abilityId)).error shouldBe null
            resolveStack()
            val decision = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val pick = findCardsInHand(1, name).single()
            decision.options.contains(pick) shouldBe true
            selectCards(listOf(pick))
        }

        /** Pass turns until player 1's upkeep, then resolve its triggers. */
        fun TestGame.toOwnUpkeep() {
            do {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            } while (state.activePlayerId != player1Id)
            resolveStack()
        }

        fun TestGame.setRefine(id: EntityId, count: Int) {
            state = state.updateEntity(id) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withCounters(CounterType.REFINE, count))
            }
        }

        fun board(spell: String, vararg extra: String, islands: Int = 1) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Uvilda, Dean of Perfection")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardInHand(1, spell)
            .apply { extra.forEach { withCardOnBattlefield(2, it) } }
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("exiles the card with three refine counters; three upkeeps later it is cast for {4} less") {
            val game = board("Uvilda Test Insight")
            game.exileWithUvilda("Uvilda Test Insight")

            val card = game.exiled("Uvilda Test Insight")!!
            withClue("exiled with three refine counters, Uvilda tapped") {
                game.refine(card) shouldBe 3
                game.state.getEntity(game.findPermanent("Uvilda, Dean of Perfection")!!)!!.has<TappedComponent>() shouldBe true
            }

            game.toOwnUpkeep()
            game.refine(card) shouldBe 2
            game.toOwnUpkeep()
            game.refine(card) shouldBe 1
            withClue("no cast offer while counters remain") { game.hasPendingDecision() shouldBe false }

            game.toOwnUpkeep()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            val life = game.getLifeTotal(1)
            game.answerYesNo(true)
            game.resolveStack()

            withClue("cast for {U} off the single Island, so it resolved") {
                game.getLifeTotal(1) shouldBe life + 5
                game.isInGraveyard(1, "Uvilda Test Insight") shouldBe true
                game.exiled("Uvilda Test Insight") shouldBe null
            }
        }

        test("declining leaves it exiled with no counters, and it is never offered again") {
            val game = board("Uvilda Test Insight")
            game.exileWithUvilda("Uvilda Test Insight")
            val card = game.exiled("Uvilda Test Insight")!!
            game.setRefine(card, 1)

            game.toOwnUpkeep()
            game.answerYesNo(false)
            game.resolveStack()
            game.exiled("Uvilda Test Insight") shouldBe card
            game.refine(card) shouldBe 0

            game.toOwnUpkeep()
            withClue("no counter left to remove, so nothing triggers") {
                game.hasPendingDecision() shouldBe false
                game.exiled("Uvilda Test Insight") shouldBe card
            }
        }

        test("the reduction is generic only: {2}{U}{U} still needs two blue") {
            val game = board("Uvilda Test Tide")
            game.exileWithUvilda("Uvilda Test Tide")
            val card = game.exiled("Uvilda Test Tide")!!
            game.setRefine(card, 1)
            val life = game.getLifeTotal(1)

            game.toOwnUpkeep()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("one Island can't pay {U}{U}; the card stays exiled") {
                game.getLifeTotal(1) shouldBe life
                game.exiled("Uvilda Test Tide") shouldBe card
            }
        }

        test("the reduction takes exactly four generic: {6}{U} costs {2}{U}") {
            val oneIsland = board("Uvilda Test Torrent")
            oneIsland.exileWithUvilda("Uvilda Test Torrent")
            val card = oneIsland.exiled("Uvilda Test Torrent")!!
            oneIsland.setRefine(card, 1)
            val life = oneIsland.getLifeTotal(1)
            oneIsland.toOwnUpkeep()
            oneIsland.answerYesNo(true)
            oneIsland.resolveStack()
            withClue("one Island can't pay {2}{U}") {
                oneIsland.getLifeTotal(1) shouldBe life
                oneIsland.exiled("Uvilda Test Torrent") shouldBe card
            }

            val threeIslands = board("Uvilda Test Torrent", islands = 3)
            threeIslands.exileWithUvilda("Uvilda Test Torrent")
            val again = threeIslands.exiled("Uvilda Test Torrent")!!
            threeIslands.setRefine(again, 1)
            threeIslands.toOwnUpkeep()
            threeIslands.answerYesNo(true)
            threeIslands.resolveStack()
            withClue("three Islands pay {2}{U}") {
                threeIslands.isInGraveyard(1, "Uvilda Test Torrent") shouldBe true
            }
        }

        test("cost increases apply before the reduction (Thalia's tax is absorbed)") {
            val game = board("Uvilda Test Spark", "Thalia, Guardian of Thraben")
            game.exileWithUvilda("Uvilda Test Spark")
            val card = game.exiled("Uvilda Test Spark")!!
            game.setRefine(card, 1)
            val life = game.getLifeTotal(1)

            game.toOwnUpkeep()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("{2}{U} + {1} − {4} = {U}, payable with one Island") {
                game.getLifeTotal(1) shouldBe life + 5
                game.isInGraveyard(1, "Uvilda Test Spark") shouldBe true
            }
        }

        test("the gained abilities end when the card leaves exile") {
            val game = board("Uvilda Test Insight")
            game.exileWithUvilda("Uvilda Test Insight")
            val card = game.exiled("Uvilda Test Insight")!!
            withClue("two abilities granted to the exiled card") {
                game.state.grantedTriggeredAbilities.count { it.entityId == card } shouldBe 2
            }
            game.setRefine(card, 1)

            game.toOwnUpkeep()
            game.answerYesNo(true)
            game.resolveStack()
            game.state.grantedTriggeredAbilities.filter { it.entityId == card }.shouldBeEmpty()
        }

        test("declining to exile anything does nothing") {
            val game = board("Uvilda Test Insight")
            val uvilda = game.findPermanent("Uvilda, Dean of Perfection")!!
            game.execute(ActivateAbility(game.player1Id, uvilda, UvildaDeanOfPerfection.activatedAbilities[0].id)).error shouldBe null
            game.resolveStack()
            game.skipSelection()
            game.isInHand(1, "Uvilda Test Insight") shouldBe true
            game.state.grantedTriggeredAbilities.shouldBeEmpty()
        }

        test("Nassari gets a +1/+1 counter when you cast a spell from exile") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nassari, Dean of Expression")
                .withLandsOnBattlefield(1, "Island", 5)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Uvilda Test Insight") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.toOwnUpkeep()
            withClue("Nassari exiled the top card of the opponent's library") {
                game.state.getExile(game.player2Id).size shouldBe 1
            }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val stolen = game.state.getExile(game.player2Id).single()
            game.execute(CastSpell(game.player1Id, stolen)).error shouldBe null
            game.resolveStack()

            val nassari = game.findPermanent("Nassari, Dean of Expression")!!
            game.state.getEntity(nassari)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
