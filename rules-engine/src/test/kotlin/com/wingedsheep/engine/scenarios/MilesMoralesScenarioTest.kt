package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.spm.cards.MilesMorales
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Miles Morales // Ultimate Spider-Man (SPM #108) —
 * a transforming double-faced Legendary Creature.
 *
 * Front — Miles Morales · {1}{G} · 1/2:
 *   ETB: put a +1/+1 counter on each of up to two target creatures.
 *   {3}{R}{G}{W}: Transform Miles Morales. Activate only as a sorcery.
 *
 * Back — Ultimate Spider-Man · 4/3, First strike, haste:
 *   Camouflage — {2}: +1/+1 counter on itself; gains hexproof and becomes colorless until EOT.
 *   Whenever you attack, double the number of each kind of counter on each Spider and legendary
 *   creature you control.
 */
class MilesMoralesScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun plusOne(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.withCounters(id: EntityId, count: Int) {
        state = state.updateEntity(id) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to count)))
        }
    }

    init {
        // The front face's only non-mana activated ability is the Transform ability.
        val transformAbilityId = MilesMorales.activatedAbilities.first { !it.isManaAbility }.id
        // The back face's only non-mana activated ability is Camouflage.
        val camouflageAbilityId = MilesMorales.backFace!!.activatedAbilities.first { !it.isManaAbility }.id

        context("Miles Morales — front face") {

            test("ETB puts a +1/+1 counter on each of up to two target creatures") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Miles Morales")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val cast = game.castSpell(1, "Miles Morales")
                withClue("Miles Morales should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the ETB; got ${game.getPendingDecision()}")
                game.selectTargets(listOf(bears, giant))
                game.resolveStack()

                withClue("Grizzly Bears gets one +1/+1 counter") { plusOne(game, bears) shouldBe 1 }
                withClue("Hill Giant gets one +1/+1 counter") { plusOne(game, giant) shouldBe 1 }
            }

            test("{3}{R}{G}{W} sorcery-speed ability transforms Miles into Ultimate Spider-Man") {
                // Cast Miles from hand so the engine attaches the DoubleFacedComponent that the
                // Transform ability needs (the scenario builder only wires it for back faces).
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Miles Morales")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Miles Morales")
                withClue("Miles Morales should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                // ETB targets are optional (up to two); take none.
                if (game.getPendingDecision() is ChooseTargetsDecision) game.selectTargets(emptyList())
                game.resolveStack()

                val miles = game.findPermanent("Miles Morales")!!
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = miles, abilityId = transformAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Miles flipped to his back face") {
                    game.state.getEntity(miles)!!.get<CardComponent>()!!.name shouldBe "Ultimate Spider-Man"
                }
            }
        }

        context("Ultimate Spider-Man — back face") {

            test("Camouflage adds a +1/+1 counter and grants hexproof + colorless until end of turn") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Ultimate Spider-Man", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spidey = game.findPermanent("Ultimate Spider-Man")!!
                withClue("starts G/R/W (color indicator)") {
                    projector.project(game.state).getColors(spidey) shouldBe setOf("GREEN", "RED", "WHITE")
                }

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = spidey, abilityId = camouflageAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a +1/+1 counter was placed") { plusOne(game, spidey) shouldBe 1 }

                val projected = projector.project(game.state)
                withClue("becomes colorless") { projected.getColors(spidey).shouldBeEmpty() }
                withClue("gains hexproof") { projected.hasKeyword(spidey, Keyword.HEXPROOF).shouldBeTrue() }
            }

            test("attacking doubles each Spider and legendary creature's counters, sparing other creatures") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Ultimate Spider-Man", summoningSickness = false) // Spider + legendary
                    .withCardOnBattlefield(1, "Pincer Spider")                                   // Spider, non-legendary
                    .withCardOnBattlefield(1, "Molimo, Maro-Sorcerer")                           // legendary, non-Spider
                    .withCardOnBattlefield(1, "Grizzly Bears")                                    // neither — control
                    .withLandsOnBattlefield(1, "Forest", 2)                                       // Molimo is */* = lands you control
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spidey = game.findPermanent("Ultimate Spider-Man")!!
                val pincer = game.findPermanent("Pincer Spider")!!
                val molimo = game.findPermanent("Molimo, Maro-Sorcerer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.withCounters(spidey, 2)
                game.withCounters(pincer, 1)
                game.withCounters(molimo, 3)
                game.withCounters(bears, 2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ultimate Spider-Man" to 2)).error shouldBe null // attack player 2
                game.resolveStack()

                withClue("attacking Spider+legendary is doubled 2 -> 4") { plusOne(game, spidey) shouldBe 4 }
                withClue("a non-attacking Spider you control is doubled 1 -> 2") { plusOne(game, pincer) shouldBe 2 }
                withClue("a legendary non-Spider you control is doubled 3 -> 6") { plusOne(game, molimo) shouldBe 6 }
                withClue("a non-Spider non-legendary creature is untouched (stays 2)") { plusOne(game, bears) shouldBe 2 }
            }
        }
    }
}
