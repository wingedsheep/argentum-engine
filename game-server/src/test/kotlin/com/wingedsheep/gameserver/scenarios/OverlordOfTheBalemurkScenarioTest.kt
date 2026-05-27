package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Overlord of the Balemurk.
 *
 * Card reference:
 * - Overlord of the Balemurk ({3}{B}{B}): Enchantment Creature — Avatar Horror 5/5
 *   Impending 5—{1}{B}
 *   Whenever this permanent enters or attacks, mill four cards, then you may return a
 *   non-Avatar creature card or a planeswalker card from your graveyard to your hand.
 */
class OverlordOfTheBalemurkScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun ScenarioTestBase.TestGame.timeCounters(perm: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(perm)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    init {
        context("Overlord of the Balemurk — enters/attacks ability") {

            test("cast for its mana cost: mills four, then may return a non-Avatar creature (Avatars excluded)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Overlord of the Balemurk")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")  // non-Avatar creature — eligible
                    .withCardInGraveyard(1, "Heedless One")   // Avatar creature — excluded by filter
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Overlord of the Balemurk")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }

                // Resolve the spell + its enters trigger; the mill happens, then the optional
                // return surfaces as a SelectCardsDecision.
                game.resolveStack()

                withClue("The enters trigger should produce the optional-return decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                val optionNames = decision.options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("Non-Avatar creature is returnable") { optionNames shouldContain "Grizzly Bears" }
                withClue("Avatar creature is excluded") { optionNames shouldNotContain "Heedless One" }

                val grizzly = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                game.selectCards(listOf(grizzly))

                withClue("Returned creature is now in hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
                withClue("Avatar stays in the graveyard") { game.isInGraveyard(1, "Heedless One") shouldBe true }
                withClue("Overlord is on the battlefield") { game.isOnBattlefield("Overlord of the Balemurk") shouldBe true }
            }

            test("cast for its impending cost: enters with five time counters and isn't a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Overlord of the Balemurk")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithAlternativeCost(1, "Overlord of the Balemurk")
                withClue("Impending cast should succeed: ${cast.error}") { cast.error shouldBe null }

                game.resolveStack()

                // Even cast for impending it enters and its ability triggers (the optional return).
                withClue("The enters trigger fires on impending entry too") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(emptyList()) // decline the optional return

                val overlord = game.findPermanent("Overlord of the Balemurk")!!
                withClue("Enters with five time counters") { game.timeCounters(overlord) shouldBe 5 }
                withClue("Isn't a creature while it has a time counter") {
                    projector.project(game.state).isCreature(overlord) shouldBe false
                }
                withClue("Still an enchantment while impending") {
                    projector.project(game.state).hasType(overlord, "ENCHANTMENT") shouldBe true
                }
            }

            test("attacking triggers the mill-and-return ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Overlord of the Balemurk")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // It's a creature on the battlefield (no time counters), so it can attack.
                projector.project(game.state).isCreature(game.findPermanent("Overlord of the Balemurk")!!) shouldBe true

                val attack = game.declareAttackers(mapOf("Overlord of the Balemurk" to 2))
                withClue("Declaring the attack should succeed: ${attack.error}") { attack.error shouldBe null }

                game.resolveStack()

                withClue("The attacks trigger should produce the optional-return decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                val grizzly = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                game.selectCards(listOf(grizzly))
                withClue("Returned creature is now in hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
            }
        }
    }
}
