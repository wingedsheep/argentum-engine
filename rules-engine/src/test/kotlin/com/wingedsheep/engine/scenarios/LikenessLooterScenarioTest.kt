package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.woe.cards.LikenessLooter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Likeness Looter (WOE #208) — {U}{B} Creature — Faerie Shapeshifter, 1/1, Rare.
 *
 * "Flying
 *  {T}: Draw a card, then discard a card.
 *  {X}: This creature becomes a copy of target creature card in your graveyard with mana value X,
 *  except it has flying and this ability. Activate only as a sorcery."
 *
 * Focus: the copy-exception riders on
 * [com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect] (CR 707.9) —
 * `addedKeywords` for "except it has flying" and `retainActivatingAbility` for "and this ability",
 * without which the copy replaces the card component wholesale and the permanent could never be
 * re-aimed. Also pins that the `{X}`-bound "mana value X" target is legal at all: the enumerator
 * has to be permissive while X is unbound, and then reject a mismatched X at activation.
 */
class LikenessLooterScenarioTest : ScenarioTestBase() {

    init {
        // The copy ability is the second activated ability ({T}: loot is the first).
        val copyAbilityId = LikenessLooter.activatedAbilities[1].id

        context("Likeness Looter") {

            // Grizzly Bears (MV 2, 2/2, no flying) and Hill Giant (MV 4, 3/3) in the graveyard give
            // two distinct mana values to aim X at.
            fun board() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Likeness Looter")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun ScenarioTestBase.TestGame.copyFromGraveyard(
                looter: EntityId,
                x: Int,
                cardName: String
            ) = execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = looter,
                    abilityId = copyAbilityId,
                    xValue = x,
                    targets = listOf(
                        ChosenTarget.Card(
                            cardId = state.getGraveyard(player1Id).first { id ->
                                state.getEntity(id)?.get<CardComponent>()?.name == cardName
                            },
                            ownerId = player1Id,
                            zone = Zone.GRAVEYARD
                        )
                    )
                )
            )

            test("copies the graveyard creature but keeps flying — the 'except it has flying' rider") {
                val game = board()
                val looter = game.findPermanent("Likeness Looter")!!

                val result = game.copyFromGraveyard(looter, x = 2, cardName = "Grizzly Bears")
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val card = game.state.getEntity(looter)!!.get<CardComponent>()!!
                withClue("it is now Grizzly Bears") { card.name shouldBe "Grizzly Bears" }
                withClue("copiable P/T come from the copied card") {
                    game.state.projectedState.getPower(looter) shouldBe 2
                    game.state.projectedState.getToughness(looter) shouldBe 2
                }
                withClue("Grizzly Bears has no flying — the copy exception puts it back") {
                    game.state.projectedState.hasKeyword(looter, Keyword.FLYING) shouldBe true
                }
            }

            test("the copy keeps this ability, so it can be re-aimed at a different mana value") {
                val game = board()
                val looter = game.findPermanent("Likeness Looter")!!

                game.copyFromGraveyard(looter, x = 2, cardName = "Grizzly Bears")
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.getEntity(looter)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"

                withClue("the retained ability is granted durably, not left on the replaced card") {
                    game.state.grantedActivatedAbilities.count {
                        it.entityId == looter && it.ability.id == copyAbilityId
                    } shouldBe 1
                }

                // Now copy the MV-4 card. This only works because the ability came back.
                val again = game.copyFromGraveyard(looter, x = 4, cardName = "Hill Giant")
                withClue("second activation should succeed: ${again.error}") { again.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val card = game.state.getEntity(looter)!!.get<CardComponent>()!!
                withClue("it is now Hill Giant") { card.name shouldBe "Hill Giant" }
                withClue("still flying") {
                    game.state.projectedState.hasKeyword(looter, Keyword.FLYING) shouldBe true
                }
                withClue("the grant is idempotent — re-activating must not stack a second copy") {
                    game.state.grantedActivatedAbilities.count {
                        it.entityId == looter && it.ability.id == copyAbilityId
                    } shouldBe 1
                }
            }

            test("a graveyard creature whose mana value isn't X is not a legal target") {
                val game = board()

                // X = 2 but Hill Giant is mana value 4.
                val looter = game.findPermanent("Likeness Looter")!!
                val result = game.copyFromGraveyard(looter, x = 2, cardName = "Hill Giant")
                withClue("mismatched mana value must be rejected at activation") {
                    (result.error != null) shouldBe true
                }
            }

            test("the ability is enumerated while X is still unbound") {
                val game = board()
                val looter = game.findPermanent("Likeness Looter")!!

                // The enumerator runs before the player picks X, so "mana value X" has to match
                // permissively — otherwise no graveyard card qualifies and the ability disappears.
                withClue("the {X} copy ability is offered") {
                    game.getLegalActions(1).any { info ->
                        val a = info.action
                        a is ActivateAbility && a.sourceId == looter && a.abilityId == copyAbilityId
                    } shouldBe true
                }
            }
        }
    }
}
