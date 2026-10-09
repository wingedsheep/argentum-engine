package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.castabilityAwareCardSelection], one test per misplay from the 2026-10-09 AI-vs-AI log
 * review. Each board is the logged one, cut down to what the choice reads; each test also pins the
 * legacy answer, so the misplay it documents stays visible should the flag ever be retired.
 */
class CastabilityAwareCardSelectionTest : FunSpec({

    val sets = listOf("LCI", "VOW", "SPM").map(MtgSetCatalog::requireByCode)
    val allCards = sets.flatMap { it.cards + it.basicLands }.distinctBy { it.name }

    /** A game at [me]'s precombat main with an empty hand; the rest of [deck] is the library. */
    fun game(deck: Deck): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(allCards)
            initMirrorMatch(deck)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        var s = d.state
        for (id in s.getZone(me, Zone.HAND)) {
            s = s.moveToZone(id, ZoneKey(me, Zone.HAND), ZoneKey(me, Zone.LIBRARY))
        }
        d.replaceState(s)
        return d to me
    }

    fun responder(d: GameTestDriver, flag: Boolean) = DecisionResponder(
        GameSimulator(d.cardRegistry),
        AIPlayer.defaultEvaluator(),
        intents = IntentCatalog.of(d.cardRegistry),
        castabilityAwareCardSelection = flag,
    )

    fun choose(d: GameTestDriver, me: EntityId, prompt: String, options: List<EntityId>, flag: Boolean): List<EntityId> {
        val decision = SelectCardsDecision(
            id = "test",
            playerId = me,
            prompt = prompt,
            context = DecisionContext(phase = DecisionPhase.RESOLUTION),
            options = options,
            minSelections = 1,
            maxSelections = 1,
        )
        return (responder(d, flag).respond(d.state, decision, me) as CardsSelectedResponse).selectedCards
    }

    fun names(d: GameTestDriver, ids: List<EntityId>) = ids.map { d.getCardName(it) }

    test("g11 T7: Orazca Puzzle-Door takes the removal, not a land, with a land already in hand") {
        val (d, me) = game(Deck.of("Plains" to 20, "Island" to 20))
        repeat(3) { d.putLandOnBattlefield(me, "Plains") }
        d.putLandOnBattlefield(me, "Island")
        d.addComponent(me, LandDropsComponent(remaining = 0, playedThisTurn = 1))
        d.putCardInHand(me, "Acrobatic Leap")
        d.putCardInHand(me, "Plains")
        d.putCardInHand(me, "Unstable Glyphbridge")
        val vein = d.putCardOnTopOfLibrary(me, "Promising Vein")
        val sawblades = d.putCardOnTopOfLibrary(me, "Spring-Loaded Sawblades")

        choose(d, me, "Choose 1 card", listOf(vein, sawblades), flag = false) shouldBe listOf(vein)
        choose(d, me, "Choose 1 card", listOf(vein, sawblades), flag = true) shouldBe listOf(sawblades)
    }

    test("g10 T11: connive keeps the only land while the land drop is unspent") {
        val (d, me) = game(Deck.of("Island" to 20, "Swamp" to 20))
        d.putLandOnBattlefield(me, "Sinister Hideout")
        d.putLandOnBattlefield(me, "Swamp")
        d.putLandOnBattlefield(me, "Suburban Sanctuary")
        d.putLandOnBattlefield(me, "Sinister Hideout")
        d.putCreatureOnBattlefield(me, "Merciless Enforcers")
        d.putCreatureOnBattlefield(me, "Mysterio's Phantasm")
        val hand = listOf(
            d.putCardInHand(me, "Beetle, Legacy Criminal"),
            d.putCardInHand(me, "News Helicopter"),
            d.putCardInHand(me, "Beetle, Legacy Criminal"),
            d.putCardInHand(me, "Doc Ock's Henchmen"),
            d.putCardInHand(me, "University Campus"),
        )

        names(d, choose(d, me, "Choose a card to discard", hand, flag = false)) shouldBe listOf("University Campus")
        val pitched = names(d, choose(d, me, "Choose a card to discard", hand, flag = true))
        pitched shouldNotContain "University Campus"
        // The spare copy of a legend is the card this hand can best afford to lose.
        pitched shouldBe listOf("Beetle, Legacy Criminal")
    }

    test("g12 T5: a one-land hand discards its seven-drop, not its three-drop") {
        val (d, me) = game(Deck.of("Island" to 20, "Swamp" to 20))
        d.putLandOnBattlefield(me, "Swamp")
        val hand = listOf(
            "Toxrill, the Corrosive", "Rot-Tide Gargantua", "Syphon Essence", "Catapult Fodder",
            "Chill of the Grave", "Screaming Swarm", "Repository Skaab", "Voldaren Bloodcaster",
        ).map { d.putCardInHand(me, it) }
        val prompt = "Discard down to 7 cards (choose 1 to discard)"

        names(d, choose(d, me, prompt, hand, flag = false)) shouldBe listOf("Syphon Essence")
        names(d, choose(d, me, prompt, hand, flag = true)) shouldBe listOf("Toxrill, the Corrosive")
    }

    test("g20 T13: with only Swamps, discard a white card rather than a castable black one") {
        val (d, me) = game(Deck.of("Plains" to 20, "Swamp" to 20))
        repeat(2) { d.putLandOnBattlefield(me, "Swamp") }
        val hand = listOf(
            "Drogskol Infantry", "Courier Bat", "Welcoming Vampire", "Desperate Farmer",
            "Traveling Minister", "Diregraf Scavenger", "Heron of Hope", "Kindly Ancestor",
        ).map { d.putCardInHand(me, it) }
        val prompt = "Discard down to 7 cards (choose 1 to discard)"

        names(d, choose(d, me, prompt, hand, flag = false)) shouldBe listOf("Desperate Farmer")
        val white = setOf(
            "Drogskol Infantry", "Welcoming Vampire", "Traveling Minister", "Heron of Hope", "Kindly Ancestor",
        )
        white shouldContain names(d, choose(d, me, prompt, hand, flag = true)).single()
    }

    test("control: a flooded hand still pitches a land") {
        val (d, me) = game(Deck.of("Plains" to 20, "Swamp" to 20))
        repeat(3) { d.putLandOnBattlefield(me, "Swamp") }
        repeat(3) { d.putLandOnBattlefield(me, "Plains") }
        val hand = listOf(
            d.putCardInHand(me, "Swamp"),
            d.putCardInHand(me, "Plains"),
            d.putCardInHand(me, "Desperate Farmer"),
            d.putCardInHand(me, "Heron of Hope"),
        )
        (names(d, choose(d, me, "Choose a card to discard", hand, flag = true)).single() in setOf("Swamp", "Plains")) shouldBe true
    }
})
