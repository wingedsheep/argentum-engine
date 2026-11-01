package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimDeckScorer
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.DraftsimSetTables
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.draftsim.withSynergyAxes
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Synergy in deck building and drafting ([DeckSynergy] over [SynergyAxes]): a payoff is worth the
 * fuel beside it in the pool, the deck scorer counts fed payoffs, and the heuristic builder's swap
 * pass trades an unfed payoff or a vanilla body for the deck that feeds itself.
 */
class DeckSynergyTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }
    fun card(name: String): CardDefinition = byName[name] ?: error("'$name' is not in the corpus")
    fun profiles(vararg names: String) = names.map { SynergyAxes.of(card(it)) }

    // Blood Artist rewards creatures dying; Viscera Seer and Village Rites sacrifice, Raise the Alarm
    // makes bodies to sacrifice. Typhoid Rats is a creature that feeds none of that.
    val payoff = "Blood Artist"

    // Every fed share is weighed by the payoff's strength ([SynergyProfile.strength]): Blood Artist's
    // one-point drain is a weaker return per feeder than the reference lord's.
    val strength by lazy { SynergyAxes.of(card(payoff)).strength }

    test("a payoff is fed by the other cards on its axis, never by itself") {
        DeckSynergy.shortfall(profiles(payoff)) shouldBe strength.plusOrMinus(1e-9)
        DeckSynergy.shortfall(profiles(payoff, "Typhoid Rats", "Typhoid Rats")) shouldBe strength.plusOrMinus(1e-9)
        DeckSynergy.fedPayoffs(profiles(payoff, "Viscera Seer")) shouldBe (strength / 3).plusOrMinus(1e-9)
        DeckSynergy.fedPayoffs(profiles(payoff, "Viscera Seer", "Village Rites", "Raise the Alarm")) shouldBe
            strength.plusOrMinus(1e-9)
        // Past FEEDERS_WANTED a payoff is simply fed: more fuel adds nothing.
        DeckSynergy.fedPayoffs(profiles(payoff, "Viscera Seer", "Village Rites", "Raise the Alarm", "Viscera Seer")) shouldBe
            strength.plusOrMinus(1e-9)
    }

    test("a feeder's gain is the share it fills in the payoffs already there, weighed by their strength") {
        val zulaport = SynergyAxes.of(card("Zulaport Cutthroat")).strength
        DeckSynergy.feedGain(SynergyAxes.of(card("Viscera Seer")), profiles(payoff)) shouldBe (strength / 3).plusOrMinus(1e-9)
        DeckSynergy.feedGain(SynergyAxes.of(card("Viscera Seer")), profiles(payoff, "Zulaport Cutthroat")) shouldBe
            ((strength + zulaport) / 3).plusOrMinus(1e-9)
        DeckSynergy.feedGain(SynergyAxes.of(card("Typhoid Rats")), profiles(payoff)) shouldBe 0.0
        DeckSynergy.feedGain(SynergyAxes.of(card("Viscera Seer")), profiles("Typhoid Rats")) shouldBe 0.0
    }

    // ----- Draftsim: the AI's draft picks and its deck builds -----

    // Every card at the same rating, so only synergy separates them.
    fun tables(names: List<String>, synergy: Boolean): DraftsimSetTables {
        val defs = names.distinct().map(::card)
        val plain = DraftsimSetTables(defs.associate { DraftsimData.nameKey(it.name) to 2.5 }, emptySet(), emptyMap())
        return if (synergy) plain.withSynergyAxes(defs) else plain
    }

    test("the pick scorer takes the outlet over an equal vanilla creature beside a payoff") {
        val names = listOf(payoff, "Viscera Seer", "Typhoid Rats")
        val pack = listOf(card("Viscera Seer"), card("Typhoid Rats")).map { it.toScorerCard() }
        val picks = listOf(card(payoff).toScorerCard())
        fun gap(synergy: Boolean): Double {
            val scores = DraftsimScorer(tables(names, synergy)).scoreBoosterAuto(pack, picks)
            return scores.getValue("Viscera Seer").total - scores.getValue("Typhoid Rats").total
        }
        gap(synergy = false) shouldBe 0.0
        gap(synergy = true) shouldBeGreaterThan 0.0
    }

    test("a payoff with no fuel in a finished pool loses pick value, but less than a rarity step") {
        val names = listOf(payoff, "Typhoid Rats")
        val pack = listOf(card(payoff).toScorerCard())
        val picks = List(42) { card("Typhoid Rats").toScorerCard() }
        fun value(synergy: Boolean) = DraftsimScorer(tables(names, synergy)).scoreBoosterAuto(pack, picks).getValue(payoff).total
        val lost = value(synergy = false) - value(synergy = true)
        lost shouldBeGreaterThan 0.0
        lost shouldBeLessThan 1.0
    }

    test("the deck scorer counts fed payoffs, and only fed ones") {
        val fed = listOf(payoff, "Zulaport Cutthroat", "Viscera Seer", "Village Rites", "Raise the Alarm") +
            List(18) { "Typhoid Rats" }
        val unfed = listOf(payoff, "Zulaport Cutthroat") + List(21) { "Typhoid Rats" }
        val names = fed + unfed
        fun score(deck: List<String>, synergy: Boolean) =
            DraftsimDeckScorer(tables(names, synergy)).scoreNonlandSet(deck.map { card(it).toScorerCard() })

        score(fed, synergy = true) shouldBeGreaterThan score(fed, synergy = false)
        score(unfed, synergy = true) shouldBe score(unfed, synergy = false)
    }

    // ----- HeuristicDeckBuilder's swap pass -----

    test("the swap pass feeds a payoff before it cuts one, and cuts one it can't feed") {
        // Blood Artist and Zulaport beside three bodies, with outlets on the bench rated a little
        // lower: the bodies are worth more one by one, the outlets more to the deck. The margins are
        // sized to these two payoffs' strength — a one-point drain is worth a small gap, not a big one.
        var index = 0
        fun slot(name: String) = PoolSlot(index++, card(name))
        val payoffs = listOf(slot(payoff), slot("Zulaport Cutthroat"))
        val selected = payoffs + List(3) { slot("Typhoid Rats") }
        val outlets = List(3) { slot("Viscera Seer") }
        fun value(slot: PoolSlot) = when (slot.card.name) {
            "Typhoid Rats" -> 2.6
            "Viscera Seer" -> 2.45
            else -> 2.9
        }

        val feed = bestSynergySwap(selected, outlets, keepPayoffs = true, value = ::value).shouldNotBeNull()
        feed.first.card.name shouldBe "Typhoid Rats"
        feed.second.card.name shouldBe "Viscera Seer"

        // Nothing on the bench feeds them: a payoff with no fuel goes for a better body.
        val bodies = List(2) { slot("Typhoid Rats") }
        val cut = bestSynergySwap(payoffs + List(3) { slot("Typhoid Rats") }, bodies, value = ::value).shouldNotBeNull()
        cut.first.card.name shouldBe payoff
        cut.second.card.name shouldBe "Typhoid Rats"

        // With no payoff in the deck there is nothing to feed, and the better body stays.
        bestSynergySwap(List(3) { slot("Typhoid Rats") }, outlets, value = ::value) shouldBe null
    }
})
