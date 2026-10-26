package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.CranialExtraction
import com.wingedsheep.mtg.sets.definitions.fut.cards.AvenMindcensor
import com.wingedsheep.mtg.sets.definitions.lea.cards.DemonicTutor
import com.wingedsheep.mtg.sets.definitions.mir.cards.RampantGrowth
import com.wingedsheep.mtg.sets.definitions.tla.cards.WanShiTongLibrarian
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aven Mindcensor (FUT #18) — "If an opponent would search a library, that player searches the
 * top four cards of that library instead." (CR 701.23f)
 */
class AvenMindcensorScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AvenMindcensor, DemonicTutor, RampantGrowth, CranialExtraction, WanShiTongLibrarian))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Buries [buried] at depth five of [player]'s library and returns the four cards above it. */
    fun GameTestDriver.buryUnderFour(player: EntityId, buried: String): Pair<EntityId, List<EntityId>> {
        val deep = putCardOnTopOfLibrary(player, buried)
        val top = (1..4).map { putCardOnTopOfLibrary(player, "Savannah Lions") }
        return deep to top
    }

    fun GameTestDriver.castTutor(caster: EntityId): SelectCardsDecision {
        giveMana(caster, Color.BLACK, 2)
        val tutor = putCardInHand(caster, "Demonic Tutor")
        val cast = castSpell(caster, tutor)
        withClue(cast.error ?: "casting Demonic Tutor failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("an opponent's search finds only the top four cards of their library") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Aven Mindcensor")
        val (deep, top) = d.buryUnderFour(d.player1, "Centaur Courser")

        val decision = d.castTutor(d.player1)

        decision.options shouldContainExactlyInAnyOrder top
        withClue("the fifth card can't be found") { decision.options shouldNotContain deep }

        d.submitCardSelection(d.player1, listOf(top.first()))
        d.getHand(d.player1) shouldContain top.first()
        withClue("the whole library is still there to shuffle") {
            d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)) shouldContain deep
        }
    }

    test("a filtered search can't find a match buried below the top four") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Aven Mindcensor")
        val buriedForest = d.putCardOnTopOfLibrary(d.player1, "Forest")
        repeat(4) { d.putCardOnTopOfLibrary(d.player1, "Savannah Lions") }

        d.giveMana(d.player1, Color.GREEN, 2)
        val spell = d.putCardInHand(d.player1, "Rampant Growth")
        val cast = d.castSpell(d.player1, spell)
        withClue(cast.error ?: "casting Rampant Growth failed") { cast.outcome shouldBe Outcome.Done }
        d.bothPass()

        withClue("no basic land is among the top four, so the search finds nothing") {
            (d.pendingDecision as? SelectCardsDecision)?.options.orEmpty() shouldBe emptyList()
            d.state.getBattlefield() shouldNotContain buriedForest
            d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)) shouldContain buriedForest
        }
    }

    test("its controller's own search sees the whole library") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Aven Mindcensor")
        val (deep, _) = d.buryUnderFour(d.player1, "Centaur Courser")
        val librarySize = d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).size

        val decision = d.castTutor(d.player1)

        decision.options shouldContain deep
        decision.options.size shouldBe librarySize
    }

    test("searching another player's library is limited too, but not their graveyard or hand") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Aven Mindcensor")
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inHand = d.putCardInHand(d.player2, "Centaur Courser")
        val (deep, _) = d.buryUnderFour(d.player2, "Centaur Courser")
        val nearTop = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")

        d.giveMana(d.player1, Color.BLACK, 4)
        val spell = d.putCardInHand(d.player1, "Cranial Extraction")
        val cast = d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Player(d.player2)))
        withClue(cast.error ?: "casting Cranial Extraction failed") { cast.outcome shouldBe Outcome.Done }
        d.bothPass()
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player1, OptionChosenResponse(decision.id, decision.options.indexOf("Centaur Courser")))

        val exile = d.getExile(d.player2)
        listOf(inGraveyard, inHand, nearTop).forEach { exile shouldContain it }
        withClue("the copy below the top four stays in the library") {
            exile shouldNotContain deep
            d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)) shouldContain deep
        }
    }

    test("searching the top four still counts as searching the library") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Aven Mindcensor")
        d.putPermanentOnBattlefield(d.player2, "Wan Shi Tong, Librarian")
        val handBefore = d.getHand(d.player2).size

        val decision = d.castTutor(d.player1)
        decision.options.size shouldBe 4
        d.submitCardSelection(d.player1, listOf(decision.options.first()))
        d.bothPass()

        withClue("Wan Shi Tong's opponent-searches trigger drew a card") {
            d.getHand(d.player2).size shouldBe handBefore + 1
        }
    }
})
