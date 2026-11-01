package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Captive Audience (RNA #160) — {5}{B}{R} Enchantment.
 *
 *   This enchantment enters under the control of an opponent of your choice.
 *   At the beginning of your upkeep, choose one that hasn't been chosen —
 *   • Your life total becomes 4.
 *   • Discard your hand.
 *   • Each opponent creates five 2/2 black Zombie creature tokens.
 *
 * Pins the entry replacement on all three entry seams — a cast spell, a card an effect returns
 * (Open the Vaults) and a token copy (Mythos of Illuna) — each arriving already controlled by the
 * chosen opponent, owned by its caster, and logged as entering under the opponent's control; and
 * the ruling that its upkeep trigger then belongs to that opponent, who loses the life and whose
 * opponents get the Zombies.
 */
class CaptiveAudienceScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseOpponent(): ChooseOptionDecision {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player1Id
        decision.prompt shouldBe "Choose an opponent"
        submitDecision(OptionChosenResponse(decision.id, 0)).error shouldBe null
        return decision
    }

    private fun TestGame.chooseMode(description: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun TestGame.assertEnteredUnderOpponent(id: EntityId) {
        withClue("controlled by the chosen opponent, from the moment it entered") {
            state.projectedState.getController(id) shouldBe player2Id
            state.getBattlefield(player2Id) shouldContain id
            state.getBattlefield(player1Id) shouldNotContain id
        }
        withClue("logged as entering under the opponent's control, not its caster's") {
            state.getEntity(player2Id)?.get<PermanentsEnteredUnderControlThisTurnComponent>()
                ?.entries.orEmpty().map { it.entityId } shouldContain id
            state.getEntity(player1Id)?.get<PermanentsEnteredUnderControlThisTurnComponent>()
                ?.entries.orEmpty().map { it.entityId } shouldNotContain id
        }
    }

    init {
        context("Captive Audience") {
            test("cast, it enters under the chosen opponent's control and its caster still owns it") {
                val game = scenario()
                    .withPlayers("Caster", "Captive")
                    .withCardInHand(1, "Captive Audience")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Captive Audience").error shouldBe null
                game.resolveStack()
                game.chooseOpponent()

                val audience = game.findPermanent("Captive Audience").shouldNotBeNull()
                game.assertEnteredUnderOpponent(audience)
                game.state.getEntity(audience)?.get<CardComponent>()?.ownerId shouldBe game.player1Id
            }

            test("its upkeep trigger is the opponent's: their life becomes 4, then the Zombies go to their opponent") {
                var builder = scenario()
                    .withPlayers("Caster", "Captive")
                    .withCardInHand(1, "Captive Audience")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) { builder = builder.withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Swamp") }
                val game = builder.build()

                game.castSpell(1, "Captive Audience").error shouldBe null
                game.resolveStack()
                game.chooseOpponent()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the opponent's upkeep") { game.state.activePlayerId shouldBe game.player2Id }
                game.resolveStack()
                val first = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                first.playerId shouldBe game.player2Id
                first.options.size shouldBe 3
                game.chooseMode("Your life total becomes 4")
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 4
                game.getLifeTotal(1) shouldBe 20

                withClue("the caster's own upkeep doesn't trigger it") {
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.stack.isEmpty() shouldBe true
                    game.hasPendingDecision() shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.resolveStack()
                val second = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                second.options shouldContainExactlyInAnyOrder listOf(
                    "Discard your hand",
                    "Each opponent creates five 2/2 black Zombie creature tokens",
                )
                game.chooseMode("Each opponent creates five 2/2 black Zombie creature tokens")
                game.resolveStack()

                val zombies = game.state.getBattlefield().filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Zombie Token" ||
                        game.state.projectedState.getSubtypes(it).contains("Zombie")
                }
                zombies.size shouldBe 5
                zombies.all { game.state.projectedState.getController(it) == game.player1Id } shouldBe true
            }

            test("returned by an effect, its owner chooses the opponent it enters under") {
                val game = scenario()
                    .withPlayers("Caster", "Captive")
                    .withCardInGraveyard(1, "Captive Audience")
                    .withCardInHand(1, "Open the Vaults")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Open the Vaults").error shouldBe null
                game.resolveStack()
                game.chooseOpponent()
                game.resolveStack()

                val audience = game.findPermanent("Captive Audience").shouldNotBeNull()
                game.state.getZone(game.player1Id, Zone.GRAVEYARD) shouldNotContain audience
                game.assertEnteredUnderOpponent(audience)
            }

            test("a token copy enters under the opponent its creator chooses") {
                val game = scenario()
                    .withPlayers("Caster", "Captive")
                    .withCardOnBattlefield(2, "Captive Audience")
                    .withCardInHand(1, "Mythos of Illuna")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val original = game.findPermanent("Captive Audience").shouldNotBeNull()
                game.castSpell(1, "Mythos of Illuna", original).error shouldBe null
                game.resolveStack()
                game.chooseOpponent()
                game.resolveStack()

                val token = game.findPermanents("Captive Audience").single { it != original }
                game.assertEnteredUnderOpponent(token)
            }
        }
    }
}
