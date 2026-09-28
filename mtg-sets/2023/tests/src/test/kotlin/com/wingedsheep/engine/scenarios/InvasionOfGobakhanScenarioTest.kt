package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.InvasionOfGobakhan
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Gobakhan // Lightshield Array (MOM #22).
 *
 * Front: the ETB looks at a target opponent's hand and may exile a nonland card; its owner may play
 * it for as long as it stays exiled, paying {2} more. Back: an end-step +1/+1 counter on each
 * creature that attacked this turn, and a sacrifice for team hexproof + indestructible.
 */
class InvasionOfGobakhanScenarioTest : ScenarioTestBase() {

    private fun exileNames(game: TestGame, playerId: com.wingedsheep.sdk.model.EntityId) =
        game.state.getExile(playerId).mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        test("ETB exiles a chosen nonland card; its owner may cast it for {2} more") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Gobakhan")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Forest")
                .withLandsOnBattlefield(2, "Mountain", 6)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Gobakhan").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            val giantId = game.findCardsInHand(2, "Hill Giant").first()
            game.selectCards(listOf(giantId))

            withClue("the chosen card is exiled; the land stays in hand") {
                exileNames(game, game.player2Id) shouldBe listOf("Hill Giant")
                game.isInHand(2, "Forest") shouldBe true
            }

            // Hand the turn to the opponent, who casts the exiled Hill Giant ({3}{R} + {2}).
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id

            game.castSpellFromExile(2, "Hill Giant").error shouldBe null
            withClue("all six lands were needed: {3}{R} plus the {2} tax") {
                game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain"
                }.count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 6
            }
            game.resolveStack()
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("the tax makes the exiled card uncastable with only its printed cost available") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Gobakhan")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Hill Giant")
                .withLandsOnBattlefield(2, "Mountain", 5)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Gobakhan").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()
            game.selectCards(listOf(game.findCardsInHand(2, "Hill Giant").first()))

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.castSpellFromExile(2, "Hill Giant").error shouldNotBe null
            exileNames(game, game.player2Id) shouldBe listOf("Hill Giant")
        }

        test("declining the exile leaves the opponent's hand intact") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Gobakhan")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Gobakhan").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()
            game.selectCards(emptyList())

            game.isInHand(2, "Hill Giant") shouldBe true
            exileNames(game, game.player2Id) shouldBe emptyList()
            game.findPermanent("Invasion of Gobakhan")!!.let { siege ->
                game.state.getEntity(siege)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) shouldBe 3
            }
        }

        test("Lightshield Array puts a +1/+1 counter on each creature that attacked this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Lightshield Array")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            withClue("a creature that didn't attack gets nothing") {
                (game.state.getEntity(giant)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }
        }

        test("sacrificing Lightshield Array gives your creatures hexproof and indestructible") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Lightshield Array")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val array = game.findPermanent("Lightshield Array")!!
            val abilityId = InvasionOfGobakhan.backFace!!.activatedAbilities.single().id
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = array, abilityId = abilityId))
                .error shouldBe null
            game.isOnBattlefield("Lightshield Array") shouldBe false
            game.resolveStack()

            val projected = game.state.projectedState
            val bears = game.findPermanent("Grizzly Bears")!!
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            withClue("an opponent's creature is unaffected") {
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.HEXPROOF) shouldBe false
            }
        }
    }
}
