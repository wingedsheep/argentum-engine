package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Soul Exchange (Fallen Empires #43) — {B}{B} Sorcery.
 *
 * "As an additional cost to cast this spell, exile a creature you control.
 *  Return target creature card from your graveyard to the battlefield. Put a +2/+2 counter on that
 *  creature if the exiled creature was a Thrull."
 *
 * The rider is the whole card: the additional cost is paid at cast time (CR 601.2h), so by the time
 * the spell resolves the question "was the exiled creature a Thrull?" can only be answered from what
 * was recorded when the cost was paid — which is what these tests pin down, in both directions.
 */
class SoulExchangeScenarioTest : ScenarioTestBase() {

    init {
        context("Soul Exchange — reanimate, with a +2/+2 counter if a Thrull paid for it") {

            test("exiling a Thrull returns the creature with a +2/+2 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Soul Exchange")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Basal Thrull")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Soul Exchange"
                }
                val thrull = game.findPermanent("Basal Thrull")!!
                val bearsCard = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id, spellId,
                        listOf(entityIdToChosenTarget(game.state, bearsCard)),
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(thrull))
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                withClue("the Thrull is exiled as the spell is cast") {
                    game.isOnBattlefield("Basal Thrull") shouldBe false
                    game.isInGraveyard(1, "Basal Thrull") shouldBe false
                }

                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")
                    ?: error("Grizzly Bears should have been returned to the battlefield")
                withClue("the exiled creature was a Thrull, so the rider applies") {
                    game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_TWO_PLUS_TWO) shouldBe 1
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }

            test("exiling a non-Thrull returns the creature with no counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Soul Exchange")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Soul Exchange"
                }
                val courser = game.findPermanent("Centaur Courser")!!
                val bearsCard = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id, spellId,
                        listOf(entityIdToChosenTarget(game.state, bearsCard)),
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(courser))
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }

                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")
                    ?: error("Grizzly Bears should have been returned to the battlefield")
                withClue("no Thrull was exiled, so the creature comes back as printed") {
                    (game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_TWO_PLUS_TWO) ?: 0) shouldBe 0
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
