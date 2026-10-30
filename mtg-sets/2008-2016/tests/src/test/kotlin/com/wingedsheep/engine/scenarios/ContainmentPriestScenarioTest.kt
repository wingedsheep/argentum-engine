package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.mtg.sets.definitions.avr.cards.Cloudshift
import com.wingedsheep.mtg.sets.definitions.avr.cards.GhostlyFlicker
import com.wingedsheep.mtg.sets.definitions.lea.cards.SolRing
import com.wingedsheep.mtg.sets.definitions.m12.cards.AdaptiveAutomaton
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MarchOfTheMachines
import com.wingedsheep.mtg.sets.definitions.nph.cards.PhyrexianMetamorph
import com.wingedsheep.mtg.sets.definitions.thb.cards.HeliodSunCrowned
import com.wingedsheep.mtg.sets.definitions.c14.cards.ContainmentPriest
import com.wingedsheep.mtg.sets.definitions.fut.cards.DryadArbor
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Containment Priest (C14 #5) — {1}{W} Creature — Human Cleric 2/2.
 *
 *   "Flash
 *    If a nontoken creature would enter and it wasn't cast, exile it instead."
 *
 * A battlefield-entry `RedirectZoneChange` filtered to `Creature.nontoken().notCast()`: reanimated,
 * flickered, manifested and land-played creatures go to exile; cast creatures and tokens enter.
 */
class ContainmentPriestScenarioTest : FunSpec({

    val manifestTop = card("Manifest Top") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.manifest() }
    }

    val returnArtifact = card("Return Artifact") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            val artifact = target(TargetFilter.ArtifactInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(artifact)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                ContainmentPriest, Zombify, Cloudshift, DryadArbor, GrizzlyBears, RaiseTheAlarm, manifestTop,
                GhostlyFlicker, SolRing, AdaptiveAutomaton, MarchOfTheMachines, PhyrexianMetamorph,
                HeliodSunCrowned, returnArtifact
            )
        )
        initMirrorMatch(deck = Deck.of("Plains" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.exile(player: EntityId) = state.getZone(player, Zone.EXILE)

    fun GameTestDriver.zombify(player: EntityId, card: EntityId) {
        val spell = putCardInHand(player, "Zombify")
        giveMana(player, Color.BLACK, 4)
        castSpellWithTargets(player, spell, listOf(ChosenTarget.Card(card, player, Zone.GRAVEYARD))).error shouldBe null
        resolveStack()
    }

    test("a creature reanimated under an opponent's Priest is exiled instead") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Containment Priest")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")

        val spell = d.putCardInHand(me, "Zombify")
        d.giveMana(me, Color.BLACK, 4)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(bears, me, Zone.GRAVEYARD))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldNotContain bears
        d.exile(me) shouldContain bears
    }

    test("a cast creature spell resolves onto the battlefield") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")

        val bears = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, bears).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldContain bears
    }

    test("tokens still enter") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val before = d.getPermanents(me).size

        val spell = d.putCardInHand(me, "Raise the Alarm")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpell(me, spell).error shouldBe null
        d.resolveStack()

        d.getPermanents(me).size shouldBe before + 2
    }

    test("a flickered creature doesn't come back — it's exiled as it would return") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val bears = d.putPermanentOnBattlefield(me, "Grizzly Bears")

        val spell = d.putCardInHand(me, "Cloudshift")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(bears))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me).none { d.getCardName(it) == "Grizzly Bears" } shouldBe true
        d.exile(me).any { d.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("a manifested card is a creature entering, so it is exiled") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val top = d.putCardOnTopOfLibrary(me, "Plains")

        val spell = d.putCardInHand(me, "Manifest Top")
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, spell).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldNotContain top
        d.exile(me) shouldContain top
    }

    test("Dryad Arbor played as a land is exiled, and the land drop is spent") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val arbor = d.putCardInHand(me, "Dryad Arbor")
        val plains = d.putCardInHand(me, "Plains")

        d.playLand(me, arbor).error shouldBe null

        d.getPermanents(me) shouldNotContain arbor
        d.exile(me) shouldContain arbor
        d.playLand(me, plains).error shouldBe "You have already played a land this turn"
    }

    test("the Priest entering without being cast doesn't exile itself") {
        val d = driver()
        val me = d.activePlayer!!
        val priest = d.putCardInGraveyard(me, "Containment Priest")

        val spell = d.putCardInHand(me, "Zombify")
        d.giveMana(me, Color.BLACK, 4)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(priest, me, Zone.GRAVEYARD))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldContain priest
    }

    for (priestFirst in listOf(true, false)) {
        test("creatures returned alongside the Priest enter with it (Priest targeted ${if (priestFirst) "first" else "second"})") {
            val d = driver()
            val me = d.activePlayer!!
            val priest = d.putPermanentOnBattlefield(me, "Containment Priest")
            val bears = d.putPermanentOnBattlefield(me, "Grizzly Bears")

            val spell = d.putCardInHand(me, "Ghostly Flicker")
            d.giveMana(me, Color.BLUE, 3)
            val targets = if (priestFirst) listOf(priest, bears) else listOf(bears, priest)
            d.castSpellWithTargets(me, spell, targets.map { ChosenTarget.Permanent(it) }).error shouldBe null
            d.resolveStack()

            val names = d.getPermanents(me).mapNotNull { d.getCardName(it) }
            names shouldContain "Containment Priest"
            names shouldContain "Grizzly Bears"
        }
    }

    test("an artifact March of the Machines makes a creature is exiled as it would enter") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        d.putPermanentOnBattlefield(me, "March of the Machines")
        val ring = d.putCardInGraveyard(me, "Sol Ring")

        val spell = d.putCardInHand(me, "Return Artifact")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(ring, me, Zone.GRAVEYARD))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldNotContain ring
        d.exile(me) shouldContain ring
    }

    test("a Heliod short of devotion isn't a creature as it would enter, so it stays") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest") // {1}{W}: devotion 1 + Heliod's 1
        val heliod = d.putCardInGraveyard(me, "Heliod, Sun-Crowned")

        d.zombify(me, heliod)

        d.getPermanents(me) shouldContain heliod
    }

    test("Phyrexian Metamorph is offered its copy first; copying a noncreature artifact, it enters") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val ring = d.putPermanentOnBattlefield(me, "Sol Ring")
        val metamorph = d.putCardInGraveyard(me, "Phyrexian Metamorph")

        d.zombify(me, metamorph)

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(me, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(ring)))
        d.resolveStack()

        d.getPermanents(me) shouldContain metamorph
        d.state.projectedState.isCreature(metamorph) shouldBe false
    }

    test("a creature that will be exiled asks no \"as this enters\" question") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val automaton = d.putCardInGraveyard(me, "Adaptive Automaton")

        d.zombify(me, automaton)

        d.isPaused shouldBe false
        d.exile(me) shouldContain automaton
    }
})
