package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.DraftsimSetTables
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.draftsim.withSynergyAxes
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Card asks: a payoff that names the kind of card it wants — "a noncreature spell", "an enchantment
 * spell", "artifact spells", "a creature with power 3 or greater" — is fed by exactly the cards that
 * filter matches, read off the printed cards rather than off a fixed list of axes.
 */
class CardAsksTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }
    fun card(name: String): CardDefinition = byName[name] ?: error("'$name' is not in the corpus")
    fun read(name: String) = SynergyAxes.of(card(name))
    fun answers(asker: String, answer: String) = read(asker).asks.any { read(answer).answers(it) }

    test("a payoff asks with its own filter, and the printed cards that match answer it") {
        // "Whenever you cast a noncreature spell": every noncreature card, not only instants.
        read("Monastery Mentor").asks shouldHaveSize 1
        answers("Monastery Mentor", "Bonesplitter") shouldBe true
        answers("Monastery Mentor", "Pacifism") shouldBe true
        answers("Monastery Mentor", "Lightning Bolt") shouldBe true
        answers("Monastery Mentor", "Grizzly Bears") shouldBe false
        // Prowess is the same ask.
        answers("Monastery Swiftspear", "Pacifism") shouldBe true
        // "Whenever you cast an enchantment spell" / "Artifact spells you cast cost {1} less".
        answers("Sigil of the Empty Throne", "Pacifism") shouldBe true
        answers("Sigil of the Empty Throne", "Bonesplitter") shouldBe false
        answers("Etherium Sculptor", "Bonesplitter") shouldBe true
        answers("Etherium Sculptor", "Pacifism") shouldBe false
        // "Whenever another creature with power 3 or greater you control enters": printed power.
        answers("Garruk's Packleader", "Garruk's Packleader") shouldBe true
        answers("Garruk's Packleader", "Grizzly Bears") shouldBe false
    }

    test("an ask every deck answers is no ask") {
        // "Creatures you control get +1/+1": every limited deck feeds it.
        read("Glorious Anthem").asks.shouldBeEmpty()
        read("Lightning Bolt").asks.shouldBeEmpty()
        read("Grizzly Bears").asks.shouldBeEmpty()
        // Landfall: the deck's lands feed it.
        read("Lotus Cobra").asks.shouldBeEmpty()
        // "Other creatures you control have hexproof" beside its planeswalkers: never short of fuel.
        read("Shalai, Voice of Plenty").asks.shouldBeEmpty()
        // Treasures are tokens; no printed card in a deck is one.
        read("Goldspan Dragon").asks.shouldBeEmpty()
    }

    test("an answer with a payoff rider is never short of fuel") {
        // A Pacifism that exiles the creature when a Vehicle you control attacks.
        val brights = read("Caught in the Brights")
        brights.asks shouldHaveSize 1
        brights.standsAlone shouldBe true
        DeckSynergy.shortfall(listOf(brights)) shouldBe 0.0
        DeckSynergy.shortfall(listOf(read("Monastery Mentor"))) shouldBe read("Monastery Mentor").strength
    }

    test("a payoff is fed by the cards its filter matches, never by itself") {
        // Fed shares, before [DeckSynergy.fedPayoffs] weighs them by the payoff's strength.
        val strength = read("Monastery Mentor").strength
        fun fed(vararg names: String) = DeckSynergy.fedPayoffs(names.map(::read)) / strength
        fed("Monastery Mentor") shouldBe 0.0
        fed("Monastery Mentor", "Grizzly Bears", "Grizzly Bears", "Grizzly Bears") shouldBe 0.0
        fed("Monastery Mentor", "Bonesplitter", "Pacifism") shouldBe (2 / DeckSynergy.FEEDERS_WANTED).plusOrMinus(1e-9)
        fed("Monastery Mentor", "Bonesplitter", "Pacifism", "Lightning Bolt") shouldBe 1.0.plusOrMinus(1e-9)
    }

    test("beside a noncreature payoff, the pick scorer takes an artifact over an equal creature") {
        val names = listOf("Monastery Mentor", "Bonesplitter", "Grizzly Bears")
        val defs = names.map(::card)
        val plain = DraftsimSetTables(defs.associate { DraftsimData.nameKey(it.name) to 2.5 }, emptySet(), emptyMap())
        val pack = listOf(card("Bonesplitter"), card("Grizzly Bears")).map { it.toScorerCard() }
        val picks = listOf(card("Monastery Mentor").toScorerCard())
        fun gap(tables: DraftsimSetTables): Double {
            val scores = DraftsimScorer(tables).scoreBoosterAuto(pack, picks)
            return scores.getValue("Bonesplitter").total - scores.getValue("Grizzly Bears").total
        }
        gap(plain.withSynergyAxes(defs)) shouldBeGreaterThan gap(plain)
    }
})
