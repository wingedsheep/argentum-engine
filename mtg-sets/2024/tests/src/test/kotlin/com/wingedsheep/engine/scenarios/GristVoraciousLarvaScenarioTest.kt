package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Grist, Voracious Larva // Grist, the Plague Swarm (MH3 #251).
 *
 * Front: a creature you control entering from your graveyard lets you pay {G} to flip Grist.
 * Back: +1 Insect + mill two (deathtouch counter if a black card was milled); −2 destroys an
 * artifact or enchantment; −6 makes a 1/1 black and green Insect copy of each creature card in your
 * graveyard.
 */
class GristVoraciousLarvaScenarioTest : ScenarioTestBase() {

    private val front = "Grist, Voracious Larva"
    private val back = "Grist, the Plague Swarm"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun abilityId(index: Int) = cardRegistry.requireCard(back).script.activatedAbilities[index].id

    private fun tokens(game: TestGame): List<EntityId> =
        game.state.getBattlefield(game.player1Id).filter { game.state.getEntity(it)?.has<TokenComponent>() == true }

    private fun settle(game: TestGame, answerMay: Boolean) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = game.getPendingDecision()) {
                is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                is YesNoDecision -> game.answerYesNo(answerMay)
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else return
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun reanimationBoard(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, front)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInHand(1, "Unearth")
        .withCardInHand(1, "Llanowar Elves")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun plagueSwarm(extra: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, back)
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(extra)
        .build()

    init {
        test("a creature returned from your graveyard lets you pay {G} to transform Grist") {
            val game = reanimationBoard()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpellTargetingGraveyardCard(1, "Unearth", listOf(bears)).error shouldBe null
            settle(game, answerMay = true)

            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent(front) shouldBe null
            val swarm = game.findPermanent(back)
            withClue("Grist returned transformed") { swarm shouldNotBe null }
            loyalty(game, swarm!!) shouldBe 3
            game.state.getEntity(swarm)?.get<CardComponent>()?.ownerId shouldBe game.player1Id
        }

        test("declining to pay leaves Grist on its front face") {
            val game = reanimationBoard()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpellTargetingGraveyardCard(1, "Unearth", listOf(bears)).error shouldBe null
            settle(game, answerMay = false)

            game.findPermanent(front) shouldNotBe null
            game.findPermanent(back) shouldBe null
        }

        test("a creature cast from hand doesn't trigger Grist") {
            val game = reanimationBoard()
            game.castSpell(1, "Llanowar Elves").error shouldBe null
            withClue("no may-pay prompt for a creature that didn't come from a graveyard") {
                game.resolveStack()
                game.getPendingDecision() shouldBe null
                game.state.stack.isEmpty() shouldBe true
            }
            game.findPermanent("Llanowar Elves") shouldNotBe null
            game.findPermanent(front) shouldNotBe null
        }

        test("a creature reanimated from an opponent's graveyard doesn't trigger Grist") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInHand(1, "Vat Emergence")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            val spell = game.state.getHand(game.player1Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Vat Emergence"
            }
            game.execute(
                CastSpell(game.player1Id, spell, listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD)))
            ).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldNotBe null
            withClue("no may-pay prompt: the Bears came from Bob's graveyard, not Alice's") {
                game.getPendingDecision() shouldBe null
                game.state.stack.isEmpty() shouldBe true
            }
            game.findPermanent(front) shouldNotBe null
        }

        test("+1 milling a black card puts a deathtouch counter on the Insect") {
            val game = plagueSwarm {
                withCardInLibrary(1, "Zombify")
                withCardInLibrary(1, "Zombify")
                withCardInLibrary(1, "Zombify")
            }
            val grist = game.findPermanent(back)!!
            game.execute(ActivateAbility(game.player1Id, grist, abilityId(0))).error shouldBe null
            game.resolveStack()

            loyalty(game, grist) shouldBe 4
            game.graveyardSize(1) shouldBe 2
            val insect = tokens(game).single()
            val projected = game.state.projectedState
            projected.getPower(insect) shouldBe 1
            projected.hasSubtype(insect, "Insect") shouldBe true
            projected.hasColor(insect, Color.BLACK) shouldBe true
            projected.hasColor(insect, Color.GREEN) shouldBe true
            game.state.getEntity(insect)?.get<CountersComponent>()?.getCount(CounterType.DEATHTOUCH) shouldBe 1
        }

        test("+1 milling no black card leaves the Insect without a counter") {
            val game = plagueSwarm {
                withCardInLibrary(1, "Grizzly Bears")
                withCardInLibrary(1, "Grizzly Bears")
                withCardInLibrary(1, "Grizzly Bears")
            }
            val grist = game.findPermanent(back)!!
            game.execute(ActivateAbility(game.player1Id, grist, abilityId(0))).error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 2
            val insect = tokens(game).single()
            (game.state.getEntity(insect)?.get<CountersComponent>()?.getCount(CounterType.DEATHTOUCH) ?: 0) shouldBe 0
        }

        test("−2 destroys target artifact") {
            val game = plagueSwarm { withCardOnBattlefield(2, "Ornithopter") }
            val grist = game.findPermanent(back)!!
            val thopter = game.findPermanent("Ornithopter")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, grist, abilityId(1),
                    targets = listOf(ChosenTarget.Permanent(thopter)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanent("Ornithopter") shouldBe null
            loyalty(game, grist) shouldBe 1
        }

        test("−6 makes a 1/1 black and green Insect copy of each creature card in your graveyard") {
            val game = plagueSwarm {
                withCardInGraveyard(1, "Hill Giant")
                withCardInGraveyard(1, "Grizzly Bears")
                withCardInGraveyard(1, "Zombify")
                withCardInGraveyard(2, "Grizzly Bears")
            }
            val grist = game.findPermanent(back)!!
            // 7 loyalty so Grist survives the −6: at 0 it would die to SBAs before resolution and,
            // as a creature card in your graveyard, correctly get a copy of its own.
            game.state = game.state.updateEntity(grist) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
            }
            game.execute(ActivateAbility(game.player1Id, grist, abilityId(2))).error shouldBe null
            game.resolveStack()

            val copies = tokens(game)
            withClue("one token per creature card in your graveyard; the sorcery and the opponent's card don't count") {
                copies.size shouldBe 2
            }
            val names = copies.map { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()
            names shouldBe setOf("Hill Giant", "Grizzly Bears")
            val projected = game.state.projectedState
            for (copy in copies) {
                projected.getPower(copy) shouldBe 1
                projected.getToughness(copy) shouldBe 1
                projected.getSubtypes(copy) shouldBe setOf("Insect")
                projected.hasColor(copy, Color.BLACK) shouldBe true
                projected.hasColor(copy, Color.GREEN) shouldBe true
                projected.hasColor(copy, Color.RED) shouldBe false
                projected.isCreature(copy) shouldBe true
            }
            withClue("the cards stay in the graveyard") { game.graveyardSize(1) shouldBe 3 }
        }
    }
}
