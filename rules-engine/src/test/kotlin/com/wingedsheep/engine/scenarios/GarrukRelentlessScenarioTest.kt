package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Garruk Relentless // Garruk, the Veil-Cursed (ISD #181, {3}{G}, Loyalty 3) — the flip planeswalker,
 * reprinted in Innistrad Remastered.
 *
 * Front:
 *   When Garruk has two or fewer loyalty counters on him, transform him.
 *   0: Garruk deals 3 damage to target creature. That creature deals damage equal to its power to him.
 *   0: Create a 2/2 green Wolf creature token.
 * Back (Garruk, the Veil-Cursed):
 *   +1: Create a 1/1 black Wolf creature token with deathtouch.
 *   −1: Sacrifice a creature. If you do, search your library for a creature card, reveal it, put it
 *       into your hand, then shuffle.
 *   −3: Creatures you control gain trample and get +X/+X until end of turn, where X is the number of
 *       creature cards in your graveyard.
 *
 * The flip is a state-triggered ability (CR 603.8), not an upkeep trigger — these tests are the
 * card's 2011-09-22 rulings, one apiece.
 */
class GarrukRelentlessScenarioTest : ScenarioTestBase() {

    init {
        test("the state trigger transforms him the moment he has two or fewer loyalty") {
            // Shock takes him from 3 to 1, which trips the state trigger as the Shock resolves.
            val game = garrukOnBattlefield {
                withCardInHand(1, "Shock")
                withLandsOnBattlefield(1, "Mountain", 1)
            }
            val garruk = game.findPermanent("Garruk Relentless")!!

            faceName(game, garruk) shouldBe "Garruk Relentless"
            loyalty(game, garruk) shouldBe 3

            game.castSpell(1, "Shock", garruk).error shouldBe null
            game.resolveStack()

            faceName(game, garruk) shouldBe "Garruk, the Veil-Cursed"
            withClue("transforming neither adds nor removes loyalty counters") {
                loyalty(game, garruk) shouldBe 1
            }
        }

        test("he stays on the front face while he still has three loyalty") {
            val game = garrukOnBattlefield()
            val garruk = game.findPermanent("Garruk Relentless")!!

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            faceName(game, garruk) shouldBe "Garruk Relentless"
            loyalty(game, garruk) shouldBe 3
        }

        test("the front damage ability trades with the creature, and the trade can flip him") {
            val game = garrukOnBattlefield {
                withCardOnBattlefield(2, "Grizzly Bears")
            }
            val garruk = game.findPermanent("Garruk Relentless")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activateFront(game, garruk, index = 0, targetPermanent = bears)
            game.resolveStack()

            withClue("3 damage killed the 2/2") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
            withClue("and its 2 power came back at Garruk, taking him from 3 to 1") {
                loyalty(game, garruk) shouldBe 1
            }
            withClue("1 loyalty is at or below 2, so the state trigger flipped him") {
                faceName(game, garruk) shouldBe "Garruk, the Veil-Cursed"
            }
        }

        test("the front wolf ability makes a 2/2 green Wolf and spends no loyalty") {
            val game = garrukOnBattlefield()
            val garruk = game.findPermanent("Garruk Relentless")!!

            activateFront(game, garruk, index = 1)
            game.resolveStack()

            val wolf = game.findAllPermanents("Wolf Token").first()
            game.state.projectedState.getPower(wolf) shouldBe 2
            game.state.projectedState.getToughness(wolf) shouldBe 2
            withClue("a 0-cost loyalty ability leaves him at 3, so he does not flip") {
                loyalty(game, garruk) shouldBe 3
                faceName(game, garruk) shouldBe "Garruk Relentless"
            }
        }

        test("the back face's +1 makes a 1/1 black Wolf with deathtouch") {
            val game = garrukOnBattlefield()
            val garruk = game.findPermanent("Garruk Relentless")!!
            flip(game, garruk)
            setLoyalty(game, garruk, 3)

            activateBack(game, garruk, index = 0)
            game.resolveStack()

            val wolf = game.findAllPermanents("Wolf Token").first()
            game.state.projectedState.getPower(wolf) shouldBe 1
            game.state.projectedState.getToughness(wolf) shouldBe 1
            game.state.projectedState.hasKeyword(wolf, Keyword.DEATHTOUCH) shouldBe true
            loyalty(game, garruk) shouldBe 4
        }

        test("the state trigger does not fire again once he is on the back face") {
            // The front face's state trigger must not exist on the back face, or he would flip back
            // and forth every time he crossed 2 loyalty. Transform swaps the card definition, so the
            // poller reads the back face's (empty) state triggers.
            val game = garrukOnBattlefield()
            val garruk = game.findPermanent("Garruk Relentless")!!
            flip(game, garruk)

            // Climb back over the threshold, then drop under it again, polling after each.
            setLoyalty(game, garruk, 5)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            setLoyalty(game, garruk, 1)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            faceName(game, garruk) shouldBe "Garruk, the Veil-Cursed"
        }

        test("the back face's -1 sacrifices a creature, then searches") {
            val game = garrukOnBattlefield {
                withCardOnBattlefield(1, "Grizzly Bears")
                withCardInLibrary(1, "Hill Giant")
            }
            val garruk = game.findPermanent("Garruk Relentless")!!
            flip(game, garruk)
            setLoyalty(game, garruk, 3)

            activateBack(game, garruk, index = 1)
            game.resolveStack()
            settleSelections(game)

            withClue("the only creature was sacrificed") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            withClue("so the search happened and put the creature card in hand") {
                game.isInHand(1, "Hill Giant") shouldBe true
            }
            loyalty(game, garruk) shouldBe 2
        }

        test("the back face's -1 searches nothing when you control no creature") {
            // "When that ability resolves, you must sacrifice a creature if you control one" — with
            // none to sacrifice, the "if you do" rider never fires.
            val game = garrukOnBattlefield {
                withCardInLibrary(1, "Hill Giant")
            }
            val garruk = game.findPermanent("Garruk Relentless")!!
            flip(game, garruk)
            setLoyalty(game, garruk, 3)

            activateBack(game, garruk, index = 1)
            game.resolveStack()
            settleSelections(game)

            withClue("nothing was sacrificed, so nothing was searched up") {
                game.isInHand(1, "Hill Giant") shouldBe false
                game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
            }
            withClue("the loyalty cost is still paid — the ability resolved") {
                loyalty(game, garruk) shouldBe 2
            }
        }

        test("the back face's -3 pumps by the creature cards in your graveyard and grants trample") {
            val game = garrukOnBattlefield {
                withCardOnBattlefield(1, "Grizzly Bears")
                withCardInGraveyard(1, "Hill Giant")
                withCardInGraveyard(1, "Kodama of the North Tree")
                withCardInGraveyard(1, "Lightning Bolt")
            }
            val garruk = game.findPermanent("Garruk Relentless")!!
            flip(game, garruk)
            setLoyalty(game, garruk, 5)

            activateBack(game, garruk, index = 2)
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("two creature cards in the graveyard (the Bolt does not count) → +2/+2") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            loyalty(game, garruk) shouldBe 2
        }

        test("a loyalty ability used on the front locks out the back face that same turn") {
            // "You can't activate a loyalty ability of Garruk Relentless and later that turn after he
            // transforms activate a loyalty ability of Garruk, the Veil-Cursed." The once-per-turn
            // restriction is per permanent, and transforming keeps the same entity.
            val game = garrukOnBattlefield {
                withCardOnBattlefield(2, "Grizzly Bears")
            }
            val garruk = game.findPermanent("Garruk Relentless")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activateFront(game, garruk, index = 0, targetPermanent = bears)
            game.resolveStack()
            faceName(game, garruk) shouldBe "Garruk, the Veil-Cursed"

            val before = loyalty(game, garruk)
            val backAbility = cardRegistry.getCard("Garruk, the Veil-Cursed")!!
                .script.activatedAbilities[0]
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = garruk,
                    abilityId = backAbility.id,
                )
            )

            withClue("the second loyalty activation this turn is rejected") {
                result.error shouldNotBe null
            }
            loyalty(game, garruk) shouldBe before
        }
    }

    // -------------------------------------------------------------------------------------------

    private fun garrukOnBattlefield(extra: ScenarioBuilder.() -> Unit = {}): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Garruk Relentless")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply(extra)
            .build()

    /**
     * Drop him under the threshold and let the state-trigger poller flip him. The poll happens at
     * each priority check that follows a stack resolution or a step change (CR 603.8), so advancing
     * to the postcombat main phase both polls and leaves us somewhere loyalty abilities are legal.
     */
    private fun flip(game: TestGame, garruk: EntityId) {
        setLoyalty(game, garruk, 2)
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        game.resolveStack()
        faceName(game, garruk) shouldBe "Garruk, the Veil-Cursed"
    }

    private fun activateFront(
        game: TestGame,
        source: EntityId,
        index: Int,
        targetPermanent: EntityId? = null
    ) = activate(game, source, "Garruk Relentless", index, targetPermanent)

    private fun activateBack(
        game: TestGame,
        source: EntityId,
        index: Int,
        targetPermanent: EntityId? = null
    ) = activate(game, source, "Garruk, the Veil-Cursed", index, targetPermanent)

    private fun activate(
        game: TestGame,
        source: EntityId,
        faceName: String,
        index: Int,
        targetPermanent: EntityId?
    ) {
        val ability = cardRegistry.getCard(faceName)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targetPermanent?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList()
            )
        ).error shouldBe null
    }

    /** Answer any forced card selections (the sacrifice choice, then the library search). */
    private fun settleSelections(game: TestGame) {
        repeat(4) {
            val decision = game.state.pendingDecision
            if (decision is SelectCardsDecision && decision.options.isNotEmpty()) {
                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()
            }
        }
    }

    private fun faceName(game: TestGame, id: EntityId): String? =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
