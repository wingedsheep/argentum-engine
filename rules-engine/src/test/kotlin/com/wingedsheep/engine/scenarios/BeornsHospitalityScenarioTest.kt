package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.BeornsHospitality
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Beorn's Hospitality (HOB #120) — {1}{G} Enchantment.
 *
 * Oracle: "Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature
 * you control. / {5}{G}{G}: This enchantment becomes a Bear creature in addition to its other types
 * and gains 'This creature's power and toughness are each equal to the number of lands you
 * control.' (This effect doesn't end.)"
 *
 * These cover the new dynamic-P/T path through the `Effects.BecomeCreature` facade. The case that
 * matters is the third one: a naive animate freezes the P/T at resolution, so the Bear would stay
 * at its activation-time size when a land later enters or leaves.
 */
class BeornsHospitalityScenarioTest : ScenarioTestBase() {

    /** The enchantment's only activated ability — "{5}{G}{G}: … becomes a Bear creature …". */
    private val animateAbilityId = BeornsHospitality.activatedAbilities.first { !it.isManaAbility }.id

    init {
        context("Beorn's Hospitality — {5}{G}{G} animate") {

            test("becomes a Bear creature that is still an enchantment, sized by your lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Beorn's Hospitality")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hospitality = game.findPermanent("Beorn's Hospitality")!!

                withClue("before activation it is a plain noncreature enchantment") {
                    game.state.projectedState.isCreature(hospitality) shouldBe false
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hospitality,
                        abilityId = animateAbilityId,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("it is now a Bear creature, and keeps its enchantment type") {
                    game.state.projectedState.isCreature(hospitality) shouldBe true
                    game.state.projectedState.hasType(hospitality, "ENCHANTMENT") shouldBe true
                    game.state.projectedState.hasSubtype(hospitality, "BEAR") shouldBe true
                }

                withClue("P/T equals the seven lands you control") {
                    game.state.projectedState.getPower(hospitality) shouldBe 7
                    game.state.projectedState.getToughness(hospitality) shouldBe 7
                }
            }

            test("only your own lands count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Beorn's Hospitality")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withLandsOnBattlefield(2, "Island", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hospitality = game.findPermanent("Beorn's Hospitality")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hospitality,
                        abilityId = animateAbilityId,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the opponent's five Islands do not feed the Bear") {
                    game.state.projectedState.getPower(hospitality) shouldBe 7
                }
            }

            test("the P/T keeps recomputing — playing another land grows the Bear") {
                // The landfall counter is deliberately pointed at the Grizzly Bears, so the
                // animated enchantment's size is a pure read of the CDA: an implementation that
                // froze the P/T at resolution would still say 7 here.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Beorn's Hospitality")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hospitality = game.findPermanent("Beorn's Hospitality")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hospitality,
                        abilityId = animateAbilityId,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(hospitality) shouldBe 7

                val mountain = game.findCardsInHand(1, "Mountain").single()
                game.execute(PlayLand(playerId = game.player1Id, cardId = mountain)).error shouldBe null

                withClue("landfall asks who gets the +1/+1 counter") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the counter went on the Bears, so they are 3/3") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                }
                withClue("an eighth land makes the animated enchantment an 8/8") {
                    game.state.projectedState.getPower(hospitality) shouldBe 8
                    game.state.projectedState.getToughness(hospitality) shouldBe 8
                }
            }
        }
    }
}
