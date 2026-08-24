package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Frankenstein's Monster (DRK #45).
 *
 * {X}{B}{B} Creature — Zombie 0/1
 * "As this creature enters, exile X creature cards from your graveyard. If you can't, put this
 *  creature into its owner's graveyard instead of onto the battlefield.
 *  For each creature card exiled this way, this creature enters with a +2/+0, +1/+1, or +0/+2
 *  counter on it."
 *
 * The two things worth pinning down: the exile actually happens from the graveyard, and X separate
 * counter choices land on the Monster with the right P/T arithmetic — including the asymmetric
 * kinds, which are new.
 */
class FrankensteinsMonsterScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Frankenstein's Monster") {

            test("exiles X creature cards and takes a counter for each") {
                // X = 2, choosing +2/+0 then +0/+2 → a 0/1 becomes 2/3.
                val game = scenario()
                    .withPlayers("Doctor", "Villager")
                    .withCardInHand(1, "Frankenstein's Monster")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hurloon Minotaur")
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Disenchant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Frankenstein's Monster").single(),
                        xValue = 2,
                    )
                ).error shouldBe null
                game.resolveStack()

                // Pick the two creature cards to exile. Only creature cards are offered, so the
                // Disenchant is never a candidate. (A forced selection — as many candidates as
                // slots — resolves without asking, so this step is conditional.)
                val selection = game.state.pendingDecision
                selection.shouldNotBeNull()
                if (selection is SelectCardsDecision) {
                    withClue("the Disenchant is not a creature card and is not offered") {
                        selection.options.size shouldBe 3
                    }
                    game.submitDecision(
                        CardsSelectedResponse(
                            selection.id,
                            listOf(
                                game.findCardsInGraveyard(1, "Grizzly Bears").single(),
                                game.findCardsInGraveyard(1, "Hurloon Minotaur").single(),
                            )
                        )
                    )
                }

                // Two counter choices, one per exiled card.
                repeat(2) { i ->
                    val modeDecision = game.state.pendingDecision
                    modeDecision.shouldNotBeNull()
                    modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                    // 0 = +2/+0, 1 = +1/+1, 2 = +0/+2. The same mode stays on the menu each time,
                    // so these indices mean the same thing on both picks.
                    game.submitDecision(OptionChosenResponse(modeDecision.id, if (i == 0) 0 else 2))
                }
                game.resolveStack()

                val monster = game.findPermanent("Frankenstein's Monster")!!
                val projected = projector.project(game.state)
                withClue("0/1 plus a +2/+0 and a +0/+2") {
                    projected.getPower(monster) shouldBe 2
                    projected.getToughness(monster) shouldBe 3
                }
                withClue("two creature cards left the graveyard for exile") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Hurloon Minotaur") shouldBe true
                }
                withClue("the noncreature card was never eligible and stayed put") {
                    game.isInGraveyard(1, "Disenchant") shouldBe true
                    game.isInExile(1, "Disenchant") shouldBe false
                }
            }

            test("an X the graveyard can't pay puts the Monster into the graveyard, exiling nothing") {
                // X = 3 with two creature cards to exile. "If you can't" is all-or-nothing: the
                // Monster does not enter as a smaller creature, and the graveyard keeps its cards.
                val game = scenario()
                    .withPlayers("Doctor", "Villager")
                    .withCardInHand(1, "Frankenstein's Monster")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hurloon Minotaur")
                    .withCardInGraveyard(1, "Disenchant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Frankenstein's Monster").single(),
                        xValue = 3,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("no selection is offered — the clause failed before anything was chosen") {
                    game.state.pendingDecision shouldBe null
                }
                withClue("it went to the graveyard instead of standing on the battlefield") {
                    game.findPermanent("Frankenstein's Monster") shouldBe null
                    game.isInGraveyard(1, "Frankenstein's Monster") shouldBe true
                }
                withClue("nothing was exiled — the two creature cards are still in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hurloon Minotaur") shouldBe true
                    game.isInExile(1, "Grizzly Bears") shouldBe false
                    game.isInExile(1, "Hurloon Minotaur") shouldBe false
                }
            }

            test("emptying the graveyard in response makes the Monster fail to enter") {
                // The graveyard is read as the Monster enters, not when it was cast: X = 1 is
                // payable on cast, and isn't any more once the only creature card is gone.
                val game = scenario()
                    .withPlayers("Doctor", "Villager")
                    .withCardInHand(1, "Frankenstein's Monster")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Frankenstein's Monster").single(),
                        xValue = 1,
                    )
                ).error shouldBe null

                // Stand in for the opponent's graveyard hate while the spell is still on the stack:
                // the only creature card leaves the graveyard for exile before the Monster resolves.
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.state = game.state
                    .removeFromZone(ZoneKey(game.player1Id, Zone.GRAVEYARD), bears)
                    .addToZone(ZoneKey(game.player1Id, Zone.EXILE), bears)
                game.resolveStack()

                withClue("the graveyard could no longer pay X, so the Monster never stuck") {
                    game.findPermanent("Frankenstein's Monster") shouldBe null
                    game.isInGraveyard(1, "Frankenstein's Monster") shouldBe true
                }
            }

            test("the counter choice is mandatory — no \"don't choose a mode\" escape") {
                // One counter per card exiled, not "up to one": the mode menu must offer the three
                // counter kinds and nothing else.
                val game = scenario()
                    .withPlayers("Doctor", "Villager")
                    .withCardInHand(1, "Frankenstein's Monster")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Frankenstein's Monster").single(),
                        xValue = 1,
                    )
                ).error shouldBe null
                game.resolveStack()

                val modeDecision = game.state.pendingDecision
                modeDecision.shouldNotBeNull()
                modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("only the three counter kinds — declining is not one of the options") {
                    modeDecision.options shouldBe listOf(
                        "Put a +2/+0 counter on this creature",
                        "Put a +1/+1 counter on this creature",
                        "Put a +0/+2 counter on this creature",
                    )
                }
            }

            test("X = 4 takes four counters even though there are only three modes") {
                // allowRepeat means the same mode stays on the menu, so the mode list is not a
                // ceiling on the number of picks — X is.
                val game = scenario()
                    .withPlayers("Doctor", "Villager")
                    .withCardInHand(1, "Frankenstein's Monster")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hurloon Minotaur")
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Frankenstein's Monster").single(),
                        xValue = 4,
                    )
                ).error shouldBe null
                game.resolveStack()

                // All four creature cards are forced, so no selection decision is presented.
                repeat(4) {
                    val modeDecision = game.state.pendingDecision
                    modeDecision.shouldNotBeNull()
                    modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                    // Always +1/+1, four times over.
                    game.submitDecision(OptionChosenResponse(modeDecision.id, 1))
                }
                game.resolveStack()

                val monster = game.findPermanent("Frankenstein's Monster")
                monster.shouldNotBeNull()
                val projected = projector.project(game.state)
                withClue("0/1 plus four +1/+1 counters") {
                    projected.getPower(monster) shouldBe 4
                    projected.getToughness(monster) shouldBe 5
                }
            }

            test("X = 0 enters as a plain 0/1 with no choices to make") {
                val game = scenario()
                    .withPlayers("Doctor", "Villager")
                    .withCardInHand(1, "Frankenstein's Monster")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Frankenstein's Monster").single(),
                        xValue = 0,
                    )
                ).error shouldBe null
                game.resolveStack()

                val monster = game.findPermanent("Frankenstein's Monster")
                monster.shouldNotBeNull()
                val projected = projector.project(game.state)
                withClue("no counters, so the printed 0/1 stands") {
                    projected.getPower(monster) shouldBe 0
                    projected.getToughness(monster) shouldBe 1
                }
                withClue("and the graveyard is untouched") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
