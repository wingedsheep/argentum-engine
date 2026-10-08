package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * "X target creatures" means exactly X targets; "up to X target creatures" means zero to X. The
 * exact form is `TargetObject.dynamicMinCount` = `dynamicMaxCount` = X, authored as
 * `targets(filter, exactly = X)`. Each rule below is pinned against real cards:
 *
 *  - CR 601.2b / 601.2c — X is announced before targets, and once the number of targets is
 *    determined it doesn't change: a cast with fewer or more targets than X is illegal (and per
 *    CR 601.6 the game rewinds — the engine rejects the action). Icy Blast (mana X), Foggy Swamp
 *    Visions (waterbend X, an additional cost), Candelabra of Tawnos (activated ability, CR 602.2b).
 *  - CR 115.3 — one object can't fill two slots of one "target", so an X above the number of legal
 *    targets can't be cast, and the legal action caps the X it offers there.
 *  - X = 0 is a legal choice and means no targets (Volcanic Eruption's 2004-10-04 ruling).
 *  - CR 608.2b — a target that became illegal is skipped; the spell still resolves for the rest.
 *  - CR 603.3d — a triggered ability's count is fixed as it goes on the stack (Lost in the Maze's
 *    X from the cast); with too few legal targets no legal choice exists and it's removed.
 */
class ExactXTargetCountScenarioTest : ScenarioTestBase() {

    private fun TestGame.handCardId(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)?.has<TappedComponent>() == true

    private fun TestGame.tap(id: EntityId) {
        state = state.updateEntity(id) { it.with(TappedComponent) }
    }

    private fun bearsGame(spell: String, islands: Int, bears: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, spell)
        .withLandsOnBattlefield(1, "Island", islands)
        .apply { repeat(bears) { withCardOnBattlefield(2, "Grizzly Bears") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("cast validation — exactly X targets (CR 601.2c)") {
            test("X = 2 with two targets casts and taps both") {
                val game = bearsGame("Icy Blast", islands = 3, bears = 3)
                val bears = game.findAllPermanents("Grizzly Bears")
                val cast = game.execute(
                    CastSpell(game.player1Id, game.handCardId("Icy Blast"), bears.take(2).map { ChosenTarget.Permanent(it) }, xValue = 2)
                )
                withClue("X = 2 with two targets is the exact count: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                bears.take(2).all { game.isTapped(it) } shouldBe true
                game.isTapped(bears[2]) shouldBe false
            }

            test("fewer targets than X is rejected — 'X target' is not 'up to X target'") {
                val game = bearsGame("Icy Blast", islands = 3, bears = 3)
                val bears = game.findAllPermanents("Grizzly Bears")
                val cast = game.execute(
                    CastSpell(game.player1Id, game.handCardId("Icy Blast"), listOf(ChosenTarget.Permanent(bears[0])), xValue = 2)
                )
                cast.error.shouldNotBeNull() shouldContain "Not enough targets"
                withClue("the rejected cast leaves the card in hand") { game.isInHand(1, "Icy Blast") shouldBe true }
            }

            test("more targets than X is rejected") {
                val game = bearsGame("Icy Blast", islands = 3, bears = 3)
                val bears = game.findAllPermanents("Grizzly Bears")
                val cast = game.execute(
                    CastSpell(game.player1Id, game.handCardId("Icy Blast"), bears.map { ChosenTarget.Permanent(it) }, xValue = 2)
                )
                cast.error shouldNotBe null
            }

            test("X = 0 is a legal cast with no targets, and it resolves rather than fizzling") {
                val game = bearsGame("Icy Blast", islands = 1, bears = 2)
                val cast = game.execute(CastSpell(game.player1Id, game.handCardId("Icy Blast"), emptyList(), xValue = 0))
                withClue("X = 0 means zero targets: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(1, "Icy Blast") shouldBe true
                game.findAllPermanents("Grizzly Bears").none { game.isTapped(it) } shouldBe true
            }

            test("X = 0 with a target is rejected — zero means zero") {
                val game = bearsGame("Icy Blast", islands = 1, bears = 1)
                val bear = game.findAllPermanents("Grizzly Bears").single()
                val cast = game.execute(
                    CastSpell(game.player1Id, game.handCardId("Icy Blast"), listOf(ChosenTarget.Permanent(bear)), xValue = 0)
                )
                cast.error shouldNotBe null
            }

            test("an X above the number of legal targets can't be cast (CR 115.3)") {
                val game = bearsGame("Icy Blast", islands = 4, bears = 2)
                val bears = game.findAllPermanents("Grizzly Bears")
                val cast = game.execute(
                    CastSpell(game.player1Id, game.handCardId("Icy Blast"), bears.map { ChosenTarget.Permanent(it) }, xValue = 3)
                )
                cast.error.shouldNotBeNull() shouldContain "Not enough targets"
            }

            test("waterbend {X}: the additional cost's X fixes the target count too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Foggy Swamp Visions")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val inGraveyard = game.findCardsInGraveyard(1, "Grizzly Bears")
                val visions = game.handCardId("Foggy Swamp Visions")

                val short = game.execute(
                    CastSpell(game.player1Id, visions, listOf(ChosenTarget.Card(inGraveyard[0], game.player1Id, Zone.GRAVEYARD)), xValue = 2)
                )
                withClue("waterbend X = 2 with one target is a short cast") {
                    short.error.shouldNotBeNull() shouldContain "Not enough targets"
                }

                val exact = game.execute(
                    CastSpell(
                        game.player1Id, visions,
                        inGraveyard.map { ChosenTarget.Card(it, game.player1Id, Zone.GRAVEYARD) },
                        xValue = 2,
                    )
                )
                withClue("waterbend X = 2 with two targets casts: ${exact.error}") { exact.error shouldBe null }
            }

            test("an activated ability's X fixes its target count (CR 602.2b) — Candelabra of Tawnos") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Candelabra of Tawnos")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val forests = game.findAllPermanents("Forest")
                forests.forEach { game.tap(it) }
                val offer = game.getLegalActions(1).first {
                    (it.action as? ActivateAbility)?.sourceId == game.findPermanent("Candelabra of Tawnos")
                }
                withClue("the ability is flagged exact so the client requires X picks") {
                    offer.xConstrainsTargetCount shouldBe true
                    offer.xConstrainsTargetCountExactly shouldBe true
                }
                val base = offer.action as ActivateAbility

                val short = game.execute(base.copy(targets = listOf(ChosenTarget.Permanent(forests[0])), xValue = 2))
                short.error.shouldNotBeNull() shouldContain "Not enough targets"

                val exact = game.execute(base.copy(targets = forests.take(2).map { ChosenTarget.Permanent(it) }, xValue = 2))
                withClue("X = 2 with two lands activates: ${exact.error}") { exact.error shouldBe null }
                game.resolveStack()
                forests.take(2).none { game.isTapped(it) } shouldBe true
                game.isTapped(forests[2]) shouldBe true
            }
        }

        context("legal actions") {
            test("an exact X spell is flagged exact and its X is capped at the legal targets") {
                val game = bearsGame("Icy Blast", islands = 6, bears = 2)
                val offer = game.getLegalActions(1).first {
                    (it.action as? CastSpell)?.cardId == game.handCardId("Icy Blast")
                }
                offer.hasXCost shouldBe true
                offer.xConstrainsTargetCount shouldBe true
                offer.xConstrainsTargetCountExactly shouldBe true
                withClue("six Islands pay for X = 5, but there are only two creatures to tap") {
                    offer.maxAffordableX shouldBe 2
                }
            }

            test("an 'up to X' spell is neither flagged exact nor capped by its targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Divergent Equation")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val offer = game.getLegalActions(1).first {
                    (it.action as? CastSpell)?.cardId == game.handCardId("Divergent Equation")
                }
                offer.xConstrainsTargetCountExactly shouldBe false
                withClue("an empty graveyard still allows 'up to X' at any affordable X") {
                    offer.maxAffordableX shouldBe 3
                }
            }
        }

        context("resolution with a target gone (CR 608.2b)") {
            test("the spell still resolves for the targets that remain legal") {
                val game = bearsGame("Distorting Wake", islands = 5, bears = 2)
                val (gone, stays) = game.findAllPermanents("Grizzly Bears")
                val cast = game.execute(
                    CastSpell(
                        game.player1Id, game.handCardId("Distorting Wake"),
                        listOf(ChosenTarget.Permanent(gone), ChosenTarget.Permanent(stays)), xValue = 2
                    )
                )
                withClue("X = 2 with two targets casts: ${cast.error}") { cast.error shouldBe null }

                game.state = game.zones.moveToZone(game.state, gone, Zone.GRAVEYARD).state
                game.resolveStack()

                withClue("the remaining legal target is still returned") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.findAllPermanents("Grizzly Bears").isEmpty() shouldBe true
                }
                game.isInGraveyard(1, "Distorting Wake") shouldBe true
            }
        }

        context("triggered abilities lock the count as they go on the stack (CR 603.3d)") {
            fun mazeGame(bears: Int, islands: Int) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lost in the Maze")
                .withLandsOnBattlefield(1, "Island", islands)
                .apply { repeat(bears) { withCardOnBattlefield(2, "Grizzly Bears") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("the X paid for the permanent becomes both the floor and the cap of the trigger's targets") {
                val game = mazeGame(bears = 3, islands = 4)
                game.castXSpell(1, "Lost in the Maze", xValue = 2).error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val requirement = decision.targetRequirements.single()
                requirement.minTargets shouldBe 2
                requirement.maxTargets shouldBe 2

                val bears = game.findAllPermanents("Grizzly Bears")
                withClue("one target is fewer than X") {
                    game.selectTargets(bears.take(1)).error shouldNotBe null
                }
                game.selectTargets(bears.take(2)).error shouldBe null
                game.resolveStack()
                bears.take(2).all { game.isTapped(it) } shouldBe true
            }

            test("with fewer legal targets than X the trigger has no legal choice and is removed") {
                val game = mazeGame(bears = 2, islands = 5)
                game.castXSpell(1, "Lost in the Maze", xValue = 3).error shouldBe null
                game.resolveStack()
                withClue("no target decision is raised, and nothing is tapped") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                    game.findAllPermanents("Grizzly Bears").none { game.isTapped(it) } shouldBe true
                }
            }

            test("X = 0 puts the trigger on the stack with no targets and asks nothing") {
                val game = mazeGame(bears = 2, islands = 2)
                game.castXSpell(1, "Lost in the Maze", xValue = 0).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
                game.state.stack.isEmpty() shouldBe true
                game.findAllPermanents("Grizzly Bears").none { game.isTapped(it) } shouldBe true
            }
        }
    }
}
