package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.spm.cards.LadyOctopusInspiredInventor
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lady Octopus, Inspired Inventor (SPM) — {U} 0/2 Legendary Creature — Human Scientist Villain.
 *
 * "Whenever you draw your first or second card each turn, put an ingenuity counter on Lady Octopus.
 *  {T}: You may cast an artifact spell from your hand with mana value less than or equal to the
 *  number of ingenuity counters on Lady Octopus without paying its mana cost."
 *
 * Exercises the two [com.wingedsheep.sdk.dsl.Triggers.NthCardDrawn] triggers (first + second draw)
 * feeding an [com.wingedsheep.sdk.core.Counters.INGENUITY] counter, and the {T} gather → filter (mv
 * ≤ counter count) → choose-up-to-one → cast-without-paying pipeline.
 */
class LadyOctopusInspiredInventorScenarioTest : ScenarioTestBase() {

    // A free instant that draws three cards, so a single cast produces the 1st, 2nd and 3rd draws
    // of the turn — proving the trigger fires on the first two and not the third.
    private val drawThree = card("Draw Three Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw three cards."
        spell { effect = Effects.DrawCards(3) }
    }

    // Two vanilla artifacts distinguished only by mana value, to prove the {T} ability's mana-value
    // gate: with two ingenuity counters the mv-2 artifact is castable and the mv-3 one is not.
    private val artifactMv2 = card("Ingenuity Artifact Two") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "A cheap gadget."
    }
    private val artifactMv3 = card("Ingenuity Artifact Three") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "A pricier gadget."
    }

    private fun ingenuityCounters(game: TestGame, name: String): Int {
        val id = game.findPermanent(name) ?: error("$name not on battlefield")
        return game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.INGENUITY) ?: 0
    }

    init {
        cardRegistry.register(LadyOctopusInspiredInventor)
        cardRegistry.register(drawThree)
        cardRegistry.register(artifactMv2)
        cardRegistry.register(artifactMv3)

        context("Lady Octopus, Inspired Inventor") {

            test("first and second draw each add an ingenuity counter; the third does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lady Octopus, Inspired Inventor", summoningSickness = false)
                    .withCardInHand(1, "Draw Three Test")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                ingenuityCounters(game, "Lady Octopus, Inspired Inventor") shouldBe 0

                // One cast draws three cards: draws #1 and #2 fire NthCardDrawn(1)/(2); #3 fires
                // neither, so exactly two ingenuity counters land.
                game.castSpell(1, "Draw Three Test").error shouldBe null
                game.resolveStack()

                withClue("first + second draw each add one counter, third adds none") {
                    ingenuityCounters(game, "Lady Octopus, Inspired Inventor") shouldBe 2
                }
            }

            test("{T} free-casts a hand artifact with mana value <= the ingenuity-counter count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lady Octopus, Inspired Inventor", summoningSickness = false)
                    .withCardInHand(1, "Draw Three Test")
                    .withCardInHand(1, "Ingenuity Artifact Two")
                    .withCardInHand(1, "Ingenuity Artifact Three")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Build up two ingenuity counters via the draw triggers.
                game.castSpell(1, "Draw Three Test").error shouldBe null
                game.resolveStack()
                ingenuityCounters(game, "Lady Octopus, Inspired Inventor") shouldBe 2

                val lady = game.findPermanent("Lady Octopus, Inspired Inventor")!!
                val mv2 = game.findCardsInHand(1, "Ingenuity Artifact Two").single()
                val mv3 = game.findCardsInHand(1, "Ingenuity Artifact Three").single()

                val abilityId = cardRegistry.getCard("Lady Octopus, Inspired Inventor")!!
                    .script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = lady, abilityId = abilityId),
                ).error shouldBe null

                // Ability resolves and pauses to choose which artifact to free-cast. Only the mv-2
                // artifact is eligible (mv <= 2 counters); the mv-3 artifact is filtered out.
                game.resolveStack()
                withClue("the {T} ability pauses for the artifact selection") {
                    (game.getPendingDecision() != null) shouldBe true
                }
                game.selectCards(listOf(mv2))
                game.resolveStack()

                withClue("the mv-2 artifact was cast without paying and is now on the battlefield") {
                    (mv2 in game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD))) shouldBe true
                    (mv2 in game.state.getZone(ZoneKey(game.player1Id, Zone.HAND))) shouldBe false
                }
                withClue("the mv-3 artifact was above the counter cap and stayed in hand") {
                    (mv3 in game.state.getZone(ZoneKey(game.player1Id, Zone.HAND))) shouldBe true
                }
                withClue("the {T} cost tapped Lady Octopus") {
                    (game.state.getEntity(lady)?.has<TappedComponent>() ?: false) shouldBe true
                }
            }
        }
    }
}
