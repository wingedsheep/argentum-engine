package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.AlongTheCrookedWay
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Along the Crooked Way (HOB #60) — {2}{B} Enchantment.
 *
 * "When this enchantment enters, return target creature card from your graveyard to your hand.
 *  Whenever a creature card leaves your graveyard, amass Goblins 1.
 *  {1}{B}: Goblins and Orcs you control gain menace until end of turn."
 *
 * The three things a lookalike implementation gets wrong: the graveyard-exit trigger is scoped to
 * *your* graveyard (an opponent reanimating pays you nothing), it is per-card rather than batched
 * ("a creature card", not "one or more"), and the menace grant reaches Orcs as well as Goblins —
 * including the amassed Army, which is a Goblin by CR 701.47a.
 */
class AlongTheCrookedWayScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        cardRegistry.register(AlongTheCrookedWay)

        context("Along the Crooked Way") {

            fun TestGame.armies(): List<EntityId> {
                val projected = stateProjector.project(state)
                return projected.getBattlefieldControlledBy(player1Id)
                    .filter { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
            }

            fun TestGame.plusOneCounters(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            test("the enters trigger returns a creature card, and that exit amasses Goblins 1") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Along the Crooked Way")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Along the Crooked Way").error shouldBe null
                game.resolveStack()

                // Enters trigger: target the only creature card in the graveyard.
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the creature card is back in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("returning it is itself a creature card leaving your graveyard") {
                    val armies = game.armies()
                    armies.size shouldBe 1
                    game.plusOneCounters(armies.single()) shouldBe 1
                    stateProjector.project(game.state)
                        .hasSubtype(armies.single(), "Goblin") shouldBe true
                }
            }

            test("a creature card leaving an opponent's graveyard amasses nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Along the Crooked Way")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(2, "Raise Dead")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Raise Dead", 2, "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                withClue("\"your graveyard\" — an opponent's reanimation is not your trigger") {
                    game.armies().size shouldBe 0
                }
            }

            test("a noncreature card leaving your graveyard amasses nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Along the Crooked Way")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInHand(1, "Regrowth")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Regrowth", 1, "Lightning Bolt")
                    .error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lightning Bolt") shouldBe true
                withClue("the filter is creature cards") { game.armies().size shouldBe 0 }
            }

            test("{1}{B} gives menace to Goblins and Orcs you control, but nothing else") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Along the Crooked Way")
                    // Goblin Piker (Goblin), Orcish Mechanics (Orc), Grizzly Bears (Bear).
                    .withCardOnBattlefield(1, "Goblin Piker", summoningSickness = false)
                    .withCardOnBattlefield(1, "Orcish Mechanics", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Goblin Piker")!!
                val orc = game.findPermanent("Orcish Mechanics")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                withClue("no menace before activation") {
                    val before = stateProjector.project(game.state)
                    before.hasKeyword(goblin, Keyword.MENACE) shouldBe false
                    before.hasKeyword(orc, Keyword.MENACE) shouldBe false
                }

                val enchantment = game.findPermanent("Along the Crooked Way")!!
                val menaceAbility = AlongTheCrookedWay.activatedAbilities.single().id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = enchantment,
                        abilityId = menaceAbility,
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val after = stateProjector.project(game.state)
                withClue("both tribes named in the text gain menace") {
                    after.hasKeyword(goblin, Keyword.MENACE) shouldBe true
                    after.hasKeyword(orc, Keyword.MENACE) shouldBe true
                }
                withClue("a Bear is neither a Goblin nor an Orc") {
                    after.hasKeyword(bear, Keyword.MENACE) shouldBe false
                }
            }
        }
    }
}
