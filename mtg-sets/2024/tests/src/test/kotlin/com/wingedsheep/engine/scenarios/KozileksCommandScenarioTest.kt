package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.KozileksCommand
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kozilek's Command (MH3 #11) — {X}{C}{C} Kindred Instant — Eldrazi.
 *
 * Choose two —
 * • Target player creates X 0/1 colorless Eldrazi Spawn creature tokens with "Sacrifice this token: Add {C}."
 * • Target player scries X, then draws a card.
 * • Exile target creature with mana value X or less.
 * • Exile up to X target cards from graveyards.
 *
 * The scry mode is the one that rides on new vocabulary — a *dynamic*, *player-scoped* scry — so it is
 * aimed at an opponent: the opponent, not the caster, must make the top/bottom decision over exactly X
 * cards of *their own* library, and the opponent draws. X = 0 must skip the scry entirely and still draw.
 */
class KozileksCommandScenarioTest : FunSpec({

    // Mode order follows the printed bullets.
    val spawn = 0
    val scryDraw = 1
    val exileCreature = 2
    val exileGraveyard = 3

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KozileksCommand, PredefinedTokens.EldraziSpawn))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castCommand(
        caster: EntityId,
        x: Int,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): ExecutionResult {
        giveColorlessMana(caster, x + 2)
        val spell = putCardInHand(caster, "Kozilek's Command")
        return submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
                xValue = x,
            )
        )
    }

    test("X=2: the targeted opponent scries 2 of their own library, orders the kept cards, and draws; caster gets two Spawn") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Opponent's library top-to-bottom: Mountain, Forest, Island, ...
        val island = driver.putCardOnTopOfLibrary(opp, "Island")
        val forest = driver.putCardOnTopOfLibrary(opp, "Forest")
        val mountain = driver.putCardOnTopOfLibrary(opp, "Mountain")
        val oppHandBefore = driver.getHandSize(opp)
        val myHandBefore = driver.getHandSize(me)

        driver.castCommand(
            me,
            x = 2,
            modes = listOf(spawn, scryDraw),
            modeTargets = listOf(
                listOf(ChosenTarget.Player(me)),
                listOf(ChosenTarget.Player(opp)),
            ),
        ).error shouldBe null
        driver.bothPass()

        // The scry decision belongs to the opponent and covers exactly the top X = 2 of their library.
        val scry = driver.pendingDecision
        scry.shouldBeInstanceOf<SelectCardsDecision>()
        scry.playerId shouldBe opp
        scry.options.shouldContainExactlyInAnyOrder(mountain, forest)

        // Opponent keeps both on top...
        driver.submitCardSelection(opp, emptyList()).error shouldBe null

        // ...and orders them themselves: Forest above Mountain.
        val reorder = driver.pendingDecision
        reorder.shouldBeInstanceOf<ReorderLibraryDecision>()
        reorder.playerId shouldBe opp
        driver.submitOrderedResponse(opp, listOf(forest, mountain)).error shouldBe null
        driver.pendingDecision shouldBe null

        // Then the opponent (not the caster) draws the Forest.
        driver.getHandSize(opp) shouldBe oppHandBefore + 1
        driver.getHand(opp) shouldContain forest
        driver.state.getLibrary(opp).take(2) shouldBe listOf(mountain, island)
        driver.getHandSize(me) shouldBe myHandBefore

        // Spawn mode: the targeted player (the caster) gets X = 2 Eldrazi Spawn.
        driver.getCreatures(me).map { driver.getCardName(it) } shouldBe listOf("Eldrazi Spawn", "Eldrazi Spawn")
        driver.getCreatures(opp).size shouldBe 0
    }

    test("X=1 scry to the bottom, combined with exiling a mana value 1 creature") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val forest = driver.putCardOnTopOfLibrary(opp, "Forest")
        val mountain = driver.putCardOnTopOfLibrary(opp, "Mountain")
        val elves = driver.putCreatureOnBattlefield(opp, "Llanowar Elves") // MV 1
        val oppHandBefore = driver.getHandSize(opp)

        driver.castCommand(
            me,
            x = 1,
            modes = listOf(scryDraw, exileCreature),
            modeTargets = listOf(
                listOf(ChosenTarget.Player(opp)),
                listOf(ChosenTarget.Permanent(elves)),
            ),
        ).error shouldBe null
        driver.bothPass()

        val scry = driver.pendingDecision
        scry.shouldBeInstanceOf<SelectCardsDecision>()
        scry.playerId shouldBe opp
        scry.options shouldBe listOf(mountain)
        driver.submitCardSelection(opp, listOf(mountain)).error shouldBe null
        while (driver.pendingDecision != null) {
            driver.pendingDecision!!.playerId shouldBe opp
            driver.autoResolveDecision()
        }

        driver.getHand(opp) shouldContain forest
        driver.getHandSize(opp) shouldBe oppHandBefore + 1
        driver.state.getLibrary(opp).last() shouldBe mountain
        driver.findPermanent(opp, "Llanowar Elves") shouldBe null
        driver.getExile(opp) shouldContain elves
    }

    test("X=0: no scry decision at all, but the targeted opponent still draws a card") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val forest = driver.putCardOnTopOfLibrary(opp, "Forest")
        val oppHandBefore = driver.getHandSize(opp)

        driver.castCommand(
            me,
            x = 0,
            modes = listOf(scryDraw, exileGraveyard),
            modeTargets = listOf(
                listOf(ChosenTarget.Player(opp)),
                emptyList(),
            ),
        ).error shouldBe null
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.getHandSize(opp) shouldBe oppHandBefore + 1
        driver.getHand(opp) shouldContain forest
    }

    test("X=2: exiles a mana value 2 creature and up to two cards from different graveyards") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears") // MV 2
        val oppCard = driver.putCardInGraveyard(opp, "Forest")
        val myCard = driver.putCardInGraveyard(me, "Island")

        driver.castCommand(
            me,
            x = 2,
            modes = listOf(exileCreature, exileGraveyard),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(bears)),
                listOf(
                    ChosenTarget.Card(oppCard, opp, Zone.GRAVEYARD),
                    ChosenTarget.Card(myCard, me, Zone.GRAVEYARD),
                ),
            ),
        ).error shouldBe null
        driver.bothPass()

        driver.findPermanent(opp, "Grizzly Bears") shouldBe null
        driver.getExile(opp).shouldContainExactlyInAnyOrder(bears, oppCard)
        driver.getExile(me) shouldContain myCard
    }

    test("a creature with mana value above X is not a legal target") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // MV 3

        driver.castCommand(
            me,
            x = 2,
            modes = listOf(exileCreature, scryDraw),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(courser)),
                listOf(ChosenTarget.Player(me)),
            ),
        ).error shouldNotBe null
    }

    test("more graveyard targets than X is refused") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val a = driver.putCardInGraveyard(opp, "Forest")
        val b = driver.putCardInGraveyard(opp, "Island")

        driver.castCommand(
            me,
            x = 1,
            modes = listOf(exileGraveyard, spawn),
            modeTargets = listOf(
                listOf(
                    ChosenTarget.Card(a, opp, Zone.GRAVEYARD),
                    ChosenTarget.Card(b, opp, Zone.GRAVEYARD),
                ),
                listOf(ChosenTarget.Player(me)),
            ),
        ).error shouldNotBe null
    }
})
