package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.commanderNinjutsuAbilities
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Yuriko, the Tiger's Shadow (C18 #52) — commander ninjutsu {U}{B} (CR 702.49d), and "whenever a
 * Ninja you control deals combat damage to a player, reveal the top card of your library and put
 * that card into your hand. Each opponent loses life equal to that card's mana value."
 *
 * Pins that the ninjutsu works from the command zone (only once there is an unblocked attacker),
 * that it also still works from the hand, and that the trigger reads the revealed card's mana value
 * and ignores non-Ninjas.
 */
class YurikoTheTigersShadowScenarioTest : ScenarioTestBase() {

    private val handAbility = commanderNinjutsuAbilities(ManaCost.parse("{U}{B}"))[0].id
    private val commandAbility = commanderNinjutsuAbilities(ManaCost.parse("{U}{B}"))[1].id

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withCardInLibrary(1, "Hill Giant")
        .withActivePlayer(1)

    /** Attack with Grizzly Bears, leave it unblocked, and hand priority back to its controller. */
    private fun TestGame.attackUnblocked() {
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers().error shouldBe null
        var guard = 0
        while (state.priorityPlayerId != player1Id && state.step == Step.DECLARE_BLOCKERS && guard++ < 4) {
            passPriority()
        }
    }

    private fun TestGame.offered(abilityId: com.wingedsheep.sdk.scripting.AbilityId) =
        getLegalActions(1).firstOrNull { (it.action as? ActivateAbility)?.abilityId == abilityId }

    private fun TestGame.passUntilPostcombat() {
        var guard = 0
        while (guard++ < 30 && (state.stack.isNotEmpty() || state.step != Step.POSTCOMBAT_MAIN)) passPriority()
    }

    init {
        context("Yuriko, the Tiger's Shadow") {

            test("commander ninjutsu from the command zone, then her trigger reveals and drains") {
                val game = base()
                    .withCardInCommandZone(1, "Yuriko, the Tiger's Shadow")
                    .withFormat(Format.Commander())
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val yuriko = game.state.getZone(game.player1Id, Zone.COMMAND).single()
                game.state = game.state.updateEntity(yuriko) { it.with(CommanderComponent(ownerId = game.player1Id)) }
                val startingLife = game.getLifeTotal(2)
                val attacker = game.findPermanent("Grizzly Bears")!!

                withClue("no unblocked attacker yet, so no ninjutsu") { game.offered(commandAbility) shouldBe null }

                game.attackUnblocked()
                val action = game.offered(commandAbility)
                withClue("offered from the command zone with the bounce cost") {
                    action shouldNotBe null
                    action!!.additionalCostInfo?.costType shouldBe "BouncePermanent"
                    action.additionalCostInfo?.validBounceTargets shouldBe listOf(attacker)
                }
                game.offered(handAbility) shouldBe null

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = yuriko,
                        abilityId = commandAbility,
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(attacker)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Yuriko left the command zone, tapped and attacking the opponent") {
                    game.state.getZone(game.player1Id, Zone.COMMAND).contains(yuriko) shouldBe false
                    game.findPermanent("Yuriko, the Tiger's Shadow") shouldBe yuriko
                    game.state.getEntity(yuriko)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(yuriko)?.get<AttackingComponent>()?.defenderId shouldBe game.player2Id
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("still her owner's commander, and putting her onto the battlefield isn't a cast (no tax)") {
                    game.state.getEntity(yuriko)?.get<CommanderComponent>()?.castsFromCommandZone shouldBe 0
                }

                game.passUntilPostcombat()

                withClue("1 combat damage (Yuriko) + Hill Giant's mana value 4") { game.getLifeTotal(2) shouldBe startingLife - 5 }
                game.isInHand(1, "Hill Giant") shouldBe true
            }

            test("commander ninjutsu still works from the hand") {
                val game = base()
                    .withCardInHand(1, "Yuriko, the Tiger's Shadow")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val yuriko = game.findCardsInHand(1, "Yuriko, the Tiger's Shadow").single()
                val attacker = game.findPermanent("Grizzly Bears")!!

                game.attackUnblocked()
                game.offered(commandAbility) shouldBe null
                game.offered(handAbility) shouldNotBe null

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = yuriko,
                        abilityId = handAbility,
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(attacker)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Yuriko, the Tiger's Shadow") shouldBe yuriko
                game.state.getEntity(yuriko)?.get<AttackingComponent>()?.defenderId shouldBe game.player2Id
            }

            test("a non-Ninja dealing combat damage doesn't trigger her") {
                val game = base()
                    .withCardOnBattlefield(1, "Yuriko, the Tiger's Shadow")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.attackUnblocked()
                game.passUntilPostcombat()

                game.getLifeTotal(2) shouldBe 20 - 2
                game.isInHand(1, "Hill Giant") shouldBe false
            }
        }
    }
}
