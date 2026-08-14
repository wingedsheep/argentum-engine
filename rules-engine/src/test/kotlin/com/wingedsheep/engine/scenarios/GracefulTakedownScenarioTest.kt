package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Graceful Takedown (WOE #171) — {1}{G} Sorcery.
 *
 *   Any number of target enchanted creatures you control and up to one other target creature you
 *   control each deal damage equal to their power to target creature you don't control.
 *
 * The card's three target requirements are declared **victim first** (see the card's notes: the
 * unbounded "any number of" slot has to be last for positional target↔requirement alignment), so the
 * `targets` list here reads victim, other-creature, then the enchanted creatures.
 *
 * What these tests pin: only the creatures *you control* deal damage — the victim, which is also one
 * of the spell's targets, must not damage itself; the damage is each dealer's own power summed; zero
 * dealers is a legal cast that does nothing; and the CR 608.2b partial-legality split from the
 * printed rulings (a dealer that became illegal drops out, an illegal victim cancels everything).
 */
class GracefulTakedownScenarioTest : ScenarioTestBase() {

    /**
     * Cast Graceful Takedown with an explicit, requirement-ordered target list:
     * `[victim] + [other?] + enchanted`.
     */
    private fun TestGame.castTakedown(
        victim: EntityId,
        other: EntityId? = null,
        enchanted: List<EntityId> = emptyList(),
    ) {
        val cardId = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Graceful Takedown"
        }
        val targets = (listOf(victim) + listOfNotNull(other) + enchanted)
            .map { ChosenTarget.Permanent(it) }
        execute(CastSpell(player1Id, cardId, targets)).error shouldBe null
    }

    private fun TestGame.settle() {
        var guard = 0
        while (guard++ < 40) {
            when (state.pendingDecision) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> {
                    if (state.stack.isEmpty()) return
                    resolveStack()
                }
                else -> error("unexpected decision: ${state.pendingDecision}")
            }
        }
        error("decision loop did not settle")
    }

    private fun TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)
            ?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
            ?.amount ?: 0

    init {
        context("Graceful Takedown") {

            test("two enchanted creatures you control each deal their power to the victim") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graceful Takedown")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3
                    .withCardOnBattlefield(1, "Pacifism")
                    .withCardOnBattlefield(1, "Holy Strength")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false) // 6/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                // Holy Strength makes the Giant a 4/5, so 2 + 4 = 6 damage.
                game.state.projectedState.getPower(giant) shouldBe 4

                game.castTakedown(victim = wurm, enchanted = listOf(bears, giant))
                game.settle()

                withClue("6 damage on a 6/4 kills it") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }
                withClue("the victim is one of the spell's targets but must not damage itself") {
                    game.damageOn(bears) shouldBe 0
                    game.damageOn(giant) shouldBe 0
                }
            }

            test("the 'up to one other' slot may be an unenchanted creature and also deals damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graceful Takedown")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(1, "Pacifism")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3, bare
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false) // 6/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castTakedown(victim = wurm, other = giant, enchanted = listOf(bears))
                game.settle()

                withClue("3 (bare Giant) + 2 (enchanted Bears) = 5 on a 6/4") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                }
            }

            test("an unenchanted creature is not a legal choice for the unbounded slot") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graceful Takedown")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Graceful Takedown"
                }

                // Two bare creatures: the first fills the "up to one other" slot, the second lands in
                // the enchanted slot and has no Aura — so the whole cast is rejected.
                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(wurm, giant, bears).map { ChosenTarget.Permanent(it) }
                    )
                )
                withClue("only one non-enchanted creature you control may be targeted") {
                    (result.error != null) shouldBe true
                }
            }

            test("targeting no creatures of your own is a legal cast that deals no damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graceful Takedown")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!

                game.castTakedown(victim = wurm)
                game.settle()

                withClue("no dealers, no damage — and the spell still resolved") {
                    game.damageOn(wurm) shouldBe 0
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isInGraveyard(1, "Graceful Takedown") shouldBe true
                }
            }

            test("a dealer that became an illegal target drops out; the rest still deal damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graceful Takedown")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3
                    .withCardOnBattlefield(1, "Pacifism")
                    .withCardOnBattlefield(1, "Holy Strength")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false) // 6/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castTakedown(victim = wurm, enchanted = listOf(bears, giant))
                // The 4/5 Giant leaves before resolution — an illegal target (CR 608.2b).
                game.state = game.state.moveToZone(
                    giant,
                    ZoneKey(game.player1Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player1Id, Zone.GRAVEYARD),
                )
                game.settle()

                withClue("only the Bears' 2 damage lands, so the 6/4 survives with 2 marked") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.damageOn(wurm) shouldBe 2
                }
            }

            test("an illegal victim means no creature deals or is dealt damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graceful Takedown")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Pacifism")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castTakedown(victim = wurm, enchanted = listOf(bears))
                game.state = game.state.moveToZone(
                    wurm,
                    ZoneKey(game.player2Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player2Id, Zone.GRAVEYARD),
                )
                game.settle()

                withClue("the dealer is still legal, but with no victim nothing is dealt damage") {
                    game.damageOn(bears) shouldBe 0
                    game.damageOn(giant) shouldBe 0
                }
            }
        }
    }
}
