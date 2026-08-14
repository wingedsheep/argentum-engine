package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vow to Erebor (HOB #31) — {1}{W} Instant.
 *
 * "Untap target creature you control. It gets +2/+2 until end of turn. If it's a Dwarf, you may
 * attach an Equipment you control to it."
 *
 * Three things worth pinning: the Dwarf rider is gated on the *target's* type (a non-Dwarf gets the
 * untap and the pump but is never offered the attach), the attach is a chosen-not-targeted
 * selection whose "up to one" shape carries the "you may", and an Equipment already attached
 * elsewhere can be moved — that is the combat trick this card is for.
 */
class VowToEreborScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    /** Cast the spell at [target] and settle the mana payment, leaving the spell mid-resolution. */
    private fun castAndResolve(game: TestGame, target: EntityId) {
        val cast = game.castSpell(1, "Vow to Erebor", target)
        withClue("Casting Vow to Erebor should succeed: ${cast.error}") { cast.error shouldBe null }
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
    }

    init {
        context("Vow to Erebor") {

            test("a Dwarf target is untapped, pumped, and may take an Equipment you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vow to Erebor")
                    .withCardOnBattlefield(1, "Dwarven Provisioner", tapped = true) // 2/2 Dwarf
                    .withCardOnBattlefield(1, "Bonesplitter")                       // +2/+0, unattached
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dwarf = game.findPermanent("Dwarven Provisioner")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                castAndResolve(game, dwarf)

                withClue("resolution pauses to offer the Equipment attach") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(bonesplitter)).error shouldBe null

                withClue("the Dwarf is untapped") { isTapped(game, dwarf) shouldBe false }
                withClue("the Bonesplitter moved onto the Dwarf") {
                    game.state.getEntity(bonesplitter)?.get<AttachedToComponent>()?.targetId shouldBe dwarf
                }
                withClue("2/2 base, +2/+2 from the spell, +2/+0 from the Bonesplitter") {
                    game.state.projectedState.getPower(dwarf) shouldBe 6
                    game.state.projectedState.getToughness(dwarf) shouldBe 4
                }
            }

            test("the attach is optional — declining still leaves the untap and the pump") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vow to Erebor")
                    .withCardOnBattlefield(1, "Dwarven Provisioner", tapped = true)
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dwarf = game.findPermanent("Dwarven Provisioner")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                castAndResolve(game, dwarf)

                game.hasPendingDecision() shouldBe true
                game.skipSelection().error shouldBe null

                withClue("declining attaches nothing") {
                    game.state.getEntity(bonesplitter)?.get<AttachedToComponent>() shouldBe null
                }
                withClue("the untap and the +2/+2 happened anyway") {
                    isTapped(game, dwarf) shouldBe false
                    game.state.projectedState.getPower(dwarf) shouldBe 4
                    game.state.projectedState.getToughness(dwarf) shouldBe 4
                }
            }

            test("a non-Dwarf target is untapped and pumped but never offered the attach") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vow to Erebor")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true) // 2/2, not a Dwarf
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                castAndResolve(game, bears)

                withClue("no Dwarf, so no Equipment prompt at all") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("untap and pump still resolve") {
                    isTapped(game, bears) shouldBe false
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                game.state.getEntity(bonesplitter)?.get<AttachedToComponent>() shouldBe null
            }

            test("an Equipment already attached elsewhere can be moved onto the Dwarf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vow to Erebor")
                    .withCardOnBattlefield(1, "Dwarven Provisioner", tapped = true)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dwarf = game.findPermanent("Dwarven Provisioner")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                withClue("the Bonesplitter starts on the Bears") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                }

                castAndResolve(game, dwarf)

                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(bonesplitter)).error shouldBe null

                withClue("the Equipment left the Bears and landed on the Dwarf") {
                    game.state.getEntity(bonesplitter)?.get<AttachedToComponent>()?.targetId shouldBe dwarf
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getPower(dwarf) shouldBe 6
                }
            }
        }
    }
}
