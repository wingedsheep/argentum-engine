package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.core.PermanentUnattachedEvent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KitsuneMystic
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kitsune Mystic // Autumn-Tail, Kitsune Sage (CHK #28) — a flip card.
 *
 * Kitsune Mystic: "At the beginning of the end step, if this creature is enchanted by two or more
 * Auras, flip it."
 * Autumn-Tail: "{1}: Attach target Aura attached to a creature to another creature."
 *
 * The new host is chosen at resolution, not targeted: only creatures the Aura could legally enchant
 * are offered (CR 701.3a), never the one it is already on, and with none the Aura stays (CR 701.3b).
 */
class KitsuneMysticScenarioTest : ScenarioTestBase() {

    private val moveAuraAbility = KitsuneMystic.flipSide!!.activatedAbilities.single().id

    private fun TestGame.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name
    private fun TestGame.hostOf(id: EntityId) = state.getEntity(id)!!.get<AttachedToComponent>()?.targetId

    /** Kitsune Mystic with Holy Strength and Pacifism on it, flipped at Bob's end step. */
    private fun flippedAutumnTail(vararg extra: Pair<Int, String>): TestGame {
        var builder = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Kitsune Mystic")
            .withCardAttachedTo(1, "Holy Strength", "Kitsune Mystic")
            .withCardAttachedTo(1, "Pacifism", "Kitsune Mystic")
            .withLandsOnBattlefield(1, "Plains", 2)
        for ((player, name) in extra) builder = builder.withCardOnBattlefield(player, name)
        val game = builder
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.resolveStack()
        return game
    }

    private fun TestGame.activateOn(aura: EntityId) {
        val sage = findPermanent("Autumn-Tail, Kitsune Sage")!!
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = sage,
                abilityId = moveAuraAbility,
                targets = listOf(ChosenTarget.Permanent(aura))
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Kitsune Mystic") {

            test("a single Aura doesn't flip it at the end step") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Kitsune Mystic")
                    .withCardAttachedTo(1, "Holy Strength", "Kitsune Mystic")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mystic = game.findPermanent("Kitsune Mystic")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.name(mystic) shouldBe "Kitsune Mystic"
            }

            test("two Auras flip it at the beginning of any player's end step") {
                val game = flippedAutumnTail()
                val sage = game.findPermanent("Autumn-Tail, Kitsune Sage")!!

                withClue("it flipped on Bob's end step, keeping both Auras") {
                    game.name(sage) shouldBe "Autumn-Tail, Kitsune Sage"
                    game.state.projectedState.isLegendary(sage) shouldBe true
                    game.state.getEntity(sage)!!.get<AttachmentsComponent>()!!.attachedIds.size shouldBe 2
                }
            }
        }

        context("Autumn-Tail, Kitsune Sage") {

            test("moves an Aura to another creature it can enchant, chosen at resolution") {
                val game = flippedAutumnTail(1 to "Grizzly Bears", 2 to "Hill Giant", 2 to "Black Knight")
                val sage = game.findPermanent("Autumn-Tail, Kitsune Sage")!!
                val holyStrength = game.findPermanent("Holy Strength")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.activateOn(holyStrength)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("another creature — not the current host, not pro-white Black Knight") {
                    decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(bears, giant)
                }

                val result = game.selectTargets(listOf(giant))
                result.error shouldBe null
                result.events.filterIsInstance<PermanentUnattachedEvent>().single().attachedToId shouldBe sage
                result.events.filterIsInstance<PermanentAttachedEvent>().single().attachedToId shouldBe giant
                game.resolveStack()

                withClue("Holy Strength now enchants Bob's Hill Giant, still controlled by Alice") {
                    game.hostOf(holyStrength) shouldBe giant
                    game.state.projectedState.getController(holyStrength) shouldBe game.player1Id
                    game.state.projectedState.getPower(giant) shouldBe 4
                    game.state.projectedState.getToughness(giant) shouldBe 5
                }
                withClue("the Sage lost the bonus and still has Pacifism") {
                    game.state.projectedState.getPower(sage) shouldBe 4
                    game.state.getEntity(sage)!!.get<AttachmentsComponent>()!!.attachedIds shouldBe
                        listOf(game.findPermanent("Pacifism")!!)
                }
            }

            test("an Aura's enchant restriction narrows the choice to its controller's creatures") {
                // Breath of Fury — "Enchant creature you control" — on Alice's Grizzly Bears.
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Kitsune Mystic")
                    .withCardAttachedTo(1, "Holy Strength", "Kitsune Mystic")
                    .withCardAttachedTo(1, "Pacifism", "Kitsune Mystic")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Breath of Fury", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                val sage = game.findPermanent("Autumn-Tail, Kitsune Sage")!!
                val fury = game.findPermanent("Breath of Fury")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.activateOn(fury)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only Alice's other creatures — never Bob's Hill Giant") {
                    decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(sage, wurm)
                }
                game.selectTargets(listOf(wurm)).error shouldBe null
                game.resolveStack()
                game.hostOf(fury) shouldBe wurm
            }

            test("with no other creature it can enchant, the Aura stays where it is") {
                val game = flippedAutumnTail(2 to "Black Knight")
                val sage = game.findPermanent("Autumn-Tail, Kitsune Sage")!!
                val holyStrength = game.findPermanent("Holy Strength")!!

                game.activateOn(holyStrength)

                withClue("no host to choose, so no decision and no move") {
                    game.getPendingDecision() shouldBe null
                    game.hostOf(holyStrength) shouldBe sage
                }
            }
        }
    }
}
