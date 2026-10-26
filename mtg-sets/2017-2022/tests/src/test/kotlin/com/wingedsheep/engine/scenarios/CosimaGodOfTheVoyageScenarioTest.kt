package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cosima, God of the Voyage // The Omenkeel (KHM #50).
 *
 *   At the beginning of your upkeep, you may exile Cosima. If you do, it gains "Whenever a land you
 *   control enters, if Cosima is exiled, you may put a voyage counter on it. If you don't, return
 *   Cosima to the battlefield with X +1/+1 counters on it and draw X cards, where X is the number of
 *   voyage counters on it."
 *
 *   The Omenkeel: Whenever a Vehicle you control deals combat damage to a player, that player exiles
 *   that many cards from the top of their library. You may play lands from among those cards for as
 *   long as they remain exiled. Crew 1
 */
class CosimaGodOfTheVoyageScenarioTest : ScenarioTestBase() {

    /** A Vehicle that is already a creature, so it can attack without crewing. */
    private val skiff = card("Omenkeel Test Skiff") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Vehicle"
        power = 3
        toughness = 3
    }

    /** Exiles a target card — a Cosima exiled this way never gained the voyage ability. */
    private val banish = card("Cosima Test Banish") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.Exile(EffectTarget.ContextTarget(0)) }
    }

    init {
        listOf(skiff, banish).forEach { cardRegistry.register(it) }

        fun TestGame.cosimaInExile(): EntityId? =
            state.getExile(player1Id).firstOrNull { state.getEntity(it)!!.get<CardComponent>()!!.name == "Cosima, God of the Voyage" }

        fun TestGame.counters(id: EntityId, type: CounterType): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

        /** Pass turns until player 1's upkeep; Cosima's upkeep trigger asks there. */
        fun TestGame.toOwnUpkeep() {
            do {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            } while (state.activePlayerId != player1Id)
            resolveStack()
        }

        fun TestGame.exileCosimaAtUpkeep() {
            toOwnUpkeep()
            getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            answerYesNo(true)
            resolveStack()
        }

        fun TestGame.playIslandFromHand() {
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val island = findCardsInHand(1, "Island").first()
            execute(PlayLand(player1Id, island)).error shouldBe null
            resolveStack()
        }

        fun cosimaBoard() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Cosima, God of the Voyage")
            .apply { repeat(3) { withCardInHand(1, "Island") } }
            .withCardInHand(1, "Cosima Test Banish")
            .apply { repeat(10) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("voyage counters accrue per land; declining returns Cosima with that many +1/+1 counters and draws that many") {
            val game = cosimaBoard()
            game.exileCosimaAtUpkeep()
            val exiled = game.cosimaInExile()
            exiled shouldNotBe null

            game.playIslandFromHand()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()
            game.counters(exiled!!, CounterType.VOYAGE) shouldBe 1

            // Next turn: Cosima is already exiled, so the upkeep trigger doesn't fire.
            game.toOwnUpkeep()
            game.hasPendingDecision() shouldBe false
            game.playIslandFromHand()
            game.answerYesNo(true)
            game.resolveStack()
            game.counters(exiled, CounterType.VOYAGE) shouldBe 2

            game.toOwnUpkeep()
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val handBefore = game.handSize(1)
            game.playIslandFromHand()
            game.answerYesNo(false)
            game.resolveStack()

            val cosima = game.findPermanent("Cosima, God of the Voyage")
            withClue("returned with two +1/+1 counters and no voyage counters; drew two") {
                cosima shouldNotBe null
                game.counters(cosima!!, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.counters(cosima, CounterType.VOYAGE) shouldBe 0
                // The Island played from hand left it; two draws came back.
                game.handSize(1) shouldBe handBefore - 1 + 2
            }
        }

        test("returning with no voyage counters gives no +1/+1 counters and draws nothing") {
            val game = cosimaBoard()
            game.exileCosimaAtUpkeep()
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val handBefore = game.handSize(1)
            game.playIslandFromHand()
            game.answerYesNo(false)
            game.resolveStack()

            val cosima = game.findPermanent("Cosima, God of the Voyage")
            cosima shouldNotBe null
            game.counters(cosima!!, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            game.handSize(1) shouldBe handBefore - 1
        }

        test("declining the upkeep exile keeps Cosima on the battlefield") {
            val game = cosimaBoard()
            game.toOwnUpkeep()
            game.answerYesNo(false)
            game.resolveStack()
            game.findPermanent("Cosima, God of the Voyage") shouldNotBe null
            game.cosimaInExile() shouldBe null
        }

        test("a Cosima exiled some other way never gained the ability, so a land entering does nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cosima, God of the Voyage")
                .withCardInHand(1, "Island")
                .withCardInHand(1, "Cosima Test Banish")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cosima = game.findPermanent("Cosima, God of the Voyage")!!
            game.castSpell(1, "Cosima Test Banish", cosima).error shouldBe null
            game.resolveStack()
            game.cosimaInExile() shouldNotBe null

            game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Island").first())).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }

        test("Cosima exiled in response to her upkeep trigger doesn't gain the ability — there was no \"if you do\"") {
            val game = cosimaBoard()
            do {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            } while (game.state.activePlayerId != game.player1Id)
            game.state.stack.size shouldBe 1
            val cosima = game.findPermanent("Cosima, God of the Voyage")!!
            game.castSpell(1, "Cosima Test Banish", cosima).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()
            game.cosimaInExile() shouldNotBe null
            game.state.grantedTriggeredAbilities.none { it.entityId == cosima } shouldBe true

            game.playIslandFromHand()
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }

        test("The Omenkeel: a Vehicle's combat damage exiles that many cards; only the lands may be played") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "The Omenkeel")
                .withCardOnBattlefield(1, "Omenkeel Test Skiff")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Lightning Bolt")
                .withCardInLibrary(2, "Mountain")
                .apply { repeat(5) { withCardInLibrary(2, "Grizzly Bears") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Omenkeel Test Skiff" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            val exiled = game.state.getExile(game.player2Id)
                .map { game.state.getEntity(it)!!.get<CardComponent>()!!.name }
            withClue("only the Vehicle's 3 damage triggers — the Bears' doesn't") {
                exiled.sorted() shouldBe listOf("Forest", "Lightning Bolt", "Mountain")
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val exiledIds = game.state.getExile(game.player2Id)
            val playable = game.getLegalActions(1).mapNotNull { (it.action as? PlayLand)?.cardId }.toSet()
            val names = playable.filter { it in exiledIds }
                .map { game.state.getEntity(it)!!.get<CardComponent>()!!.name }.sorted()
            names shouldBe listOf("Forest", "Mountain")

            val forest = exiledIds.first { game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Forest" }
            game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
            game.state.getBattlefield(game.player1Id).contains(forest) shouldBe true
            game.state.getZone(game.player1Id, Zone.BATTLEFIELD).contains(forest) shouldBe true
        }
    }
}
