package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Naga Fleshcrafter (TDM #52) — {3}{U} Snake Shapeshifter, 0/0.
 *
 * "You may have this creature enter as a copy of any creature on the battlefield.
 *  Renew — {2}{U}, Exile this card from your graveyard: Put a +1/+1 counter on target
 *  nonlegendary creature you control. Each other creature you control becomes a copy of that
 *  creature until end of turn. Activate only as a sorcery."
 *
 * The enter-as-copy is the standard Clone replacement (covered by Clone's own tests). These
 * tests focus on the renew ability and the new duration-bound, target-excluding group copy:
 *  - the target gets a +1/+1 counter and keeps its own identity (it is the copy *source*);
 *  - each OTHER creature you control becomes a copy of the target (copiable values only — the
 *    target's freshly-placed counter is not copied);
 *  - the copy lasts only until end of turn, reverting at cleanup;
 *  - a legendary creature can't be chosen as the target (nonlegendary restriction).
 */
class NagaFleshcrafterScenarioTest : ScenarioTestBase() {

    // A vanilla legendary creature to prove the "nonlegendary creature you control" target filter.
    private val legendaryHero = card("Legendary Test Hero") {
        manaCost = "{2}{W}"
        typeLine = "Legendary Creature — Human Soldier"
        power = 3
        toughness = 3
    }

    private val renewAbilityId =
        cardRegistry.getCard("Naga Fleshcrafter")!!.activatedAbilities.first().id

    private fun cardName(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): String? =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    init {
        cardRegistry.register(legendaryHero)

        context("Naga Fleshcrafter renew") {

            test("each other creature becomes a copy of the targeted creature until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Naga Fleshcrafter")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 — the copy source / target
                    .withCardOnBattlefield(1, "Hill Giant")    // 3/3 — becomes a copy
                    .withLandsOnBattlefield(1, "Island", 3)     // renew cost {2}{U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val naga = game.findCardsInGraveyard(1, "Naga Fleshcrafter").first()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = naga,
                        abilityId = renewAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Activating Naga Fleshcrafter renew should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("The targeted Grizzly Bears gets a +1/+1 counter") {
                    (game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
                }
                withClue("The target keeps its own identity (it is the copy source, not a copy)") {
                    cardName(game, bears) shouldBe "Grizzly Bears"
                }
                withClue("Each OTHER creature you control becomes a copy of the target") {
                    cardName(game, giant) shouldBe "Grizzly Bears"
                }
                withClue("The copy takes copiable values only — printed 2/2, not the counter-buffed 3/3") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
                withClue("Naga Fleshcrafter is exiled from the graveyard as part of the renew cost") {
                    game.findCardsInGraveyard(1, "Naga Fleshcrafter").size shouldBe 0
                    game.state.getExile(game.player1Id).contains(naga) shouldBe true
                }

                // Advance past this turn's cleanup — the copy is "until end of turn".
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("After end of turn the copy reverts: Hill Giant is itself again") {
                    cardName(game, giant) shouldBe "Hill Giant"
                    game.state.projectedState.getPower(giant) shouldBe 3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
                withClue("The target Grizzly Bears still has its +1/+1 counter after cleanup (it was never a copy)") {
                    (game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
                }
            }

            test("a legendary creature cannot be chosen as the renew target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Naga Fleshcrafter")
                    .withCardOnBattlefield(1, "Legendary Test Hero")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val naga = game.findCardsInGraveyard(1, "Naga Fleshcrafter").first()
                val hero = game.findPermanent("Legendary Test Hero")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = naga,
                        abilityId = renewAbilityId,
                        targets = listOf(ChosenTarget.Permanent(hero)),
                    )
                )
                withClue("Targeting a legendary creature must be rejected (nonlegendary only)") {
                    (activation.error != null) shouldBe true
                }
            }
        }
    }
}
