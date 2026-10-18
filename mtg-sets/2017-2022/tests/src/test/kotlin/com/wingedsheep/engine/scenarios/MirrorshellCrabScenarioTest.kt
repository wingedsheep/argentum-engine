package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mirrorshell Crab (NEO #63) — {5}{U}{U} Artifact Creature — Crab, 5/7.
 *
 *   Ward {3}
 *   Channel — {2}{U}, Discard this card: Counter target spell or ability unless its controller pays {3}.
 *
 * The channel counter reaches activated abilities as well as spells: the ability's controller is
 * the one asked to pay, and an unpaid ability is countered.
 */
class MirrorshellCrabScenarioTest : ScenarioTestBase() {

    private fun channelAbilityId() = cardRegistry.getCard("Mirrorshell Crab")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    init {
        fun TestGame.channelAt(target: EntityId) {
            val crab = findCardsInHand(1, "Mirrorshell Crab").first()
            execute(
                ActivateAbility(player1Id, crab, channelAbilityId(), targets = listOf(ChosenTarget.Spell(target)))
            ).error shouldBe null
        }

        /** Player 2 pings Player 1 with Prodigal Sorcerer, then passes priority to Player 1. */
        fun TestGame.pingOnStack(): EntityId {
            val sorcerer = findPermanent("Prodigal Sorcerer")!!
            val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities[0].id
            execute(
                ActivateAbility(
                    playerId = player2Id,
                    sourceId = sorcerer,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Player(player1Id)),
                )
            ).error shouldBe null
            val ping = state.stack.last()
            passPriority().error shouldBe null
            return ping
        }

        fun crabVsSorcerer(opponentMountains: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Mirrorshell Crab")
            .withLandsOnBattlefield(1, "Island", 3)
            .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
            .apply { if (opponentMountains > 0) withLandsOnBattlefield(2, "Mountain", opponentMountains) }
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Mirrorshell Crab channel") {

            test("counters an activated ability whose controller can't pay") {
                val game = crabVsSorcerer(opponentMountains = 0)
                val ping = game.pingOnStack()

                game.channelAt(ping)
                withClue("channel discards the Crab as a cost") {
                    game.isInGraveyard(1, "Mirrorshell Crab") shouldBe true
                }
                game.resolveStack()

                withClue("the ping was countered") {
                    game.state.stack.contains(ping) shouldBe false
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("asks the ability's controller to pay, and the ability resolves when they do") {
                val game = crabVsSorcerer(opponentMountains = 3)
                val ping = game.pingOnStack()

                game.channelAt(ping)
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null

                val ask = game.getPendingDecision()
                ask.shouldBeInstanceOf<YesNoDecision>()
                ask.playerId shouldBe game.player2Id
                ask.prompt shouldContain "ability"
                game.submitDecision(YesNoResponse(ask.id, true)).error shouldBe null
                val window = game.getPendingDecision()
                window.shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitDecision(ManaSourcesSelectedResponse(window.id, autoPay = true)).error shouldBe null

                withClue("the tax was paid, so the ping stays on the stack and resolves") {
                    game.state.stack.contains(ping) shouldBe true
                    game.resolveStack()
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("counters the ability when its controller declines to pay") {
                val game = crabVsSorcerer(opponentMountains = 3)
                val ping = game.pingOnStack()

                game.channelAt(ping)
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null

                val ask = game.getPendingDecision()
                ask.shouldBeInstanceOf<YesNoDecision>()
                game.submitDecision(YesNoResponse(ask.id, false)).error shouldBe null
                game.resolveStack()

                withClue("the ping was countered") {
                    game.state.stack.contains(ping) shouldBe false
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("still counters a spell whose caster declines to pay") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mirrorshell Crab")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                val bolt = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.passPriority().error shouldBe null
                game.channelAt(bolt)
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null

                val ask = game.getPendingDecision()
                ask.shouldBeInstanceOf<YesNoDecision>()
                ask.playerId shouldBe game.player2Id
                ask.prompt shouldContain "spell"
                game.submitDecision(YesNoResponse(ask.id, false)).error shouldBe null
                game.resolveStack()

                withClue("the Bolt was countered") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
