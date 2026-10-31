package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.ChandraAcolyteOfFlame
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra, Acolyte of Flame (M20 #126).
 *
 *   0: Put a loyalty counter on each red planeswalker you control.
 *   0: Create two 1/1 red Elemental creature tokens. They gain haste. Sacrifice them at the
 *      beginning of the next end step.
 *   −2: You may cast target instant or sorcery card with mana value 3 or less from your graveyard.
 *       If that spell would be put into your graveyard, exile it instead.
 *
 * Pins the colour filter on the first 0 (Chandra herself counts, a blue walker doesn't), the
 * granted haste plus delayed sacrifice on the second, and the mana-value bound on the −2.
 */
class ChandraAcolyteOfFlameScenarioTest : ScenarioTestBase() {

    init {
        context("the first 0") {

            test("puts a loyalty counter on each red planeswalker you control, Chandra included") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Acolyte of Flame")
                    .withCardOnBattlefield(1, "Chandra, Flame's Catalyst")
                    .withCardOnBattlefield(1, "Jace, the Mind Sculptor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val acolyte = game.findPermanent("Chandra, Acolyte of Flame")!!
                val catalyst = game.findPermanent("Chandra, Flame's Catalyst")!!
                val jace = game.findPermanent("Jace, the Mind Sculptor")!!
                activate(game, acolyte, index = 0)
                game.resolveStack()

                withClue("Acolyte 4 -> 5") { loyalty(game, acolyte) shouldBe 5 }
                withClue("the other red walker gains one") { loyalty(game, catalyst) shouldBe 6 }
                withClue("a blue walker is untouched") { loyalty(game, jace) shouldBe 3 }
            }
        }

        context("the second 0") {

            test("creates two hasty 1/1 red Elementals that are sacrificed at the next end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Acolyte of Flame")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val acolyte = game.findPermanent("Chandra, Acolyte of Flame")!!
                activate(game, acolyte, index = 1)
                game.resolveStack()

                val elementals = game.findPermanents("Elemental Token")
                withClue("two Elemental tokens were created") { elementals.size shouldBe 2 }
                withClue("they gained haste") {
                    elementals.all { game.state.projectedState.hasKeyword(it, Keyword.HASTE) } shouldBe true
                }
                withClue("a 0 ability leaves loyalty unchanged") { loyalty(game, acolyte) shouldBe 4 }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("both tokens were sacrificed at the end step") {
                    game.findPermanents("Elemental Token").size shouldBe 0
                }
            }
        }

        context("the −2") {

            test("casts a cheap instant from the graveyard, paying its cost, then exiles it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Acolyte of Flame")
                    .withCardInGraveyard(1, "Pyretic Ritual")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val acolyte = game.findPermanent("Chandra, Acolyte of Flame")!!
                val ritual = game.findCardsInGraveyard(1, "Pyretic Ritual").single()
                activate(game, acolyte, index = 2, targets = listOf(ChosenTarget.Card(ritual, game.player1Id, Zone.GRAVEYARD)))
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Pyretic Ritual resolved: {R}{R}{R} in the pool") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 3
                }
                withClue("its {1}{R} was paid: both Mountains are tapped") {
                    game.findPermanents("Mountain").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                withClue("it was exiled instead of going back to the graveyard") {
                    game.isInExile(1, "Pyretic Ritual") shouldBe true
                    game.findCardsInGraveyard(1, "Pyretic Ritual").size shouldBe 0
                }
                withClue("−2 took Chandra from 4 to 2") { loyalty(game, acolyte) shouldBe 2 }
            }

            test("can't target an instant or sorcery with mana value greater than 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Acolyte of Flame")
                    .withCardInGraveyard(1, "Lava Axe")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val acolyte = game.findPermanent("Chandra, Acolyte of Flame")!!
                val axe = game.findCardsInGraveyard(1, "Lava Axe").single()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = acolyte,
                        abilityId = ChandraAcolyteOfFlame.activatedAbilities[2].id,
                        targets = listOf(ChosenTarget.Card(axe, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldNotBe null
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ChandraAcolyteOfFlame.activatedAbilities[index].id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
}
