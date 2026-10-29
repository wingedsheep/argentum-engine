package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.ReplaceDamageWithMill
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Multiplicative mill-amount replacement — [ModifyKeywordActionAmount] with a `multiplier` ("If an
 * opponent would mill one or more cards, they mill twice that many cards instead", Bruvac the
 * Grandiloquent), next to the additive form ("…that many cards plus four instead", The Water
 * Crystal).
 *
 * Each applicable replacement applies once, and CR 616.1 has the affected player — the one who
 * mills — choose the order. Replacements that commute (two doublers, two "+N"s) are applied with no
 * question; a doubler next to a "+4" gives 2 → 12 or 8, so the milling player picks. Covered at every
 * mill announcement: the mill effect (one player, several players in APNAP order, a later mill in the
 * same resolution asking afresh), an activated ability's mill cost, a pay-or-suffer mill, and a
 * damage-to-mill replacement (The Mindskinner's shape).
 */
class MillAmountMultiplierReplacementTest : FunSpec({

    val doubler = card("Test Mill Doubler") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.MillEvent(Player.EachOpponent), multiplier = 2))
    }
    val plusFour = card("Test Mill Plus Four") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.MillEvent(Player.EachOpponent), modifier = 4))
    }
    val plusOne = card("Test Mill Plus One") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.MillEvent(Player.EachOpponent), modifier = 1))
    }
    val targetMillTwo = card("Target Mills Two") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Patterns.Library.mill(2, player)
        }
    }
    val targetMillsTwice = card("Target Mills Two Then One") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Patterns.Library.mill(2, player) then Patterns.Library.mill(1, player)
        }
    }
    val eachPlayerMillsTwo = card("Each Player Mills Two") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.Each)) }
    }
    val millEngine = card("Test Mill Engine") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.Mill(2))
            effect = Effects.GainLife(1)
        }
    }
    val millOrBurn = card("Mill Or Burn") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.PayOrSuffer(
                cost = PayCost.Atom(CostAtom.Mill(2)),
                suffer = Effects.LoseLife(5, EffectTarget.Controller),
            )
        }
    }
    val globalDoubler = card("Test Global Mill Doubler") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.MillEvent(Player.Each), multiplier = 2))
    }
    val globalPlusFour = card("Test Global Mill Plus Four") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.MillEvent(Player.Each), modifier = 4))
    }
    // Morph — Mill two cards: the generic CostPaymentService PayCost path (yes/no, then the order).
    val millMorph = card("Test Mill Morph") {
        manaCost = "{3}"
        typeLine = "Creature — Shapeshifter"
        power = 2
        toughness = 2
        morphCost = PayCost.Atom(CostAtom.Mill(2))
    }
    val mindskinner = card("Test Mindskinner") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(ReplaceDamageWithMill())
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                doubler, plusFour, plusOne, targetMillTwo, targetMillsTwice, eachPlayerMillsTwo,
                millEngine, millOrBurn, mindskinner, globalDoubler, globalPlusFour, millMorph,
            )
        )
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(caster: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        castSpell(caster, putCardInHand(caster, name), targets).error.shouldBeNull()
        bothPass()
    }

    /** The pending replacement-order question, asserted to be asked of [player]. */
    fun GameTestDriver.orderQuestion(player: EntityId): ChooseOptionDecision {
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player
        decision.prompt shouldBe "Choose the order your mill replacement effects apply in"
        return decision
    }

    fun GameTestDriver.choose(decision: ChooseOptionDecision, label: String) {
        val index = decision.options.indexOf(label)
        withClue("options were ${decision.options}") { (index >= 0) shouldBe true }
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error.shouldBeNull()
    }

    test("a doubler alone doubles an opponent's mill with no question") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")

        d.cast(you, "Target Mills Two", listOf(opponent))

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 4
    }

    test("two doublers commute and quadruple the mill with no question") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")

        d.cast(you, "Target Mills Two", listOf(opponent))

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 8
    }

    test("two additive replacements commute and sum with no question") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")
        d.putPermanentOnBattlefield(you, "Test Mill Plus One")

        d.cast(you, "Target Mills Two", listOf(opponent))

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 7
    }

    test("the doubler doesn't touch its controller's own mill") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.cast(you, "Target Mills Two", listOf(you))

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(you).map { d.getCardName(it) }.count { it == "Mountain" } shouldBe 2
    }

    test("a doubler next to a +4 asks the milling opponent, and doubling last mills 12") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.cast(you, "Target Mills Two", listOf(opponent))

        val decision = d.orderQuestion(opponent)
        decision.options shouldContainExactly listOf(
            "Mill 12 (Test Mill Plus Four, then Test Mill Doubler)",
            "Mill 8 (Test Mill Doubler, then Test Mill Plus Four)",
        )
        d.getGraveyard(opponent).size shouldBe 0
        d.choose(decision, "Mill 12 (Test Mill Plus Four, then Test Mill Doubler)")

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 12
    }

    test("doubling first mills 8") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.cast(you, "Target Mills Two", listOf(opponent))
        d.choose(d.orderQuestion(opponent), "Mill 8 (Test Mill Doubler, then Test Mill Plus Four)")

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 8
    }

    test("two doublers and a +4 offer each distinct result once") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.cast(you, "Target Mills Two", listOf(opponent))

        val decision = d.orderQuestion(opponent)
        // (2+4)*4, (2*2+4)*2, 2*4+4 — six orderings, three results.
        decision.options.map { it.substringBefore(" (") } shouldContainExactly listOf("Mill 24", "Mill 16", "Mill 12")
        d.choose(decision, decision.options[1])
        d.getGraveyard(opponent).size shouldBe 16
    }

    test("a second mill in the same resolution asks again rather than reusing the first answer") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.cast(you, "Target Mills Two Then One", listOf(opponent))

        d.choose(d.orderQuestion(opponent), "Mill 8 (Test Mill Doubler, then Test Mill Plus Four)")
        d.getGraveyard(opponent).size shouldBe 8
        // Mill 1: (1+4)*2 = 10 or 1*2+4 = 6.
        val second = d.orderQuestion(opponent)
        second.options.map { it.substringBefore(" (") } shouldContainExactly listOf("Mill 10", "Mill 6")
        d.choose(second, second.options[0])

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 18
    }

    test("an each-player mill asks only the affected opponent and mills everyone once") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.cast(you, "Each Player Mills Two")

        d.choose(d.orderQuestion(opponent), "Mill 12 (Test Mill Plus Four, then Test Mill Doubler)")

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(opponent).size shouldBe 12
        withClue("the caster's own mill is untouched") {
            d.getGraveyard(you).map { d.getCardName(it) }.count { it == "Mountain" } shouldBe 2
        }
    }

    test("with two affected opponents each is asked in APNAP order and the mill happens once") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(doubler, plusFour, eachPlayerMillsTwo))
        val players = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val (first, second) = d.state.turnOrderStartingWith(you).drop(1)
        players.size shouldBe 3
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")

        d.castSpell(you, d.putCardInHand(you, "Each Player Mills Two")).error.shouldBeNull()
        repeat(3) { if (d.state.stack.isNotEmpty() && d.pendingDecision == null) d.passPriority(d.state.priorityPlayerId!!) }

        d.choose(d.orderQuestion(first), "Mill 12 (Test Mill Plus Four, then Test Mill Doubler)")
        withClue("nobody mills until every affected player has answered") {
            d.getGraveyard(first).size shouldBe 0
        }
        d.choose(d.orderQuestion(second), "Mill 8 (Test Mill Doubler, then Test Mill Plus Four)")

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(first).size shouldBe 12
        d.getGraveyard(second).size shouldBe 8
        d.getGraveyard(you).map { d.getCardName(it) }.count { it == "Mountain" } shouldBe 2
    }

    test("a player who has left the game is neither asked nor resized, and the others still are") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(globalDoubler, globalPlusFour, eachPlayerMillsTwo))
        d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val (next, gone) = d.state.turnOrderStartingWith(you).drop(1)
        d.putPermanentOnBattlefield(you, "Test Global Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Global Mill Plus Four")
        d.concede(gone).error.shouldBeNull()
        withClue("the departed seat stays in turn order, which Player.Each mills iterate") {
            (gone in d.state.turnOrder) shouldBe true
            (gone in d.state.activePlayers) shouldBe false
        }

        d.castSpell(you, d.putCardInHand(you, "Each Player Mills Two")).error.shouldBeNull()
        repeat(3) { if (d.state.stack.isNotEmpty() && d.pendingDecision == null) d.passPriority(d.state.priorityPlayerId!!) }

        d.choose(d.orderQuestion(you), "Mill 12 (Test Global Mill Plus Four, then Test Global Mill Doubler)")
        d.choose(d.orderQuestion(next), "Mill 8 (Test Global Mill Doubler, then Test Global Mill Plus Four)")

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(you).map { d.getCardName(it) }.count { it == "Mountain" } shouldBe 12
        d.getGraveyard(next).size shouldBe 8
    }

    test("a client-supplied mill count no order produces is rejected before anything is paid") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Mill Doubler")
        d.putPermanentOnBattlefield(opponent, "Test Mill Plus Four")
        val engine = d.putPermanentOnBattlefield(you, "Test Mill Engine")

        val result = d.submit(
            ActivateAbility(
                you, engine, millEngine.activatedAbilities.first().id,
                costPayment = AdditionalCostPayment(millCount = 7),
            )
        )

        (result.error != null) shouldBe true
        d.isTapped(engine) shouldBe false
        d.getGraveyard(you).size shouldBe 0
        d.pendingDecision.shouldBeNull()
    }

    test("a client-supplied mill count some order produces is paid without a prompt") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Mill Doubler")
        d.putPermanentOnBattlefield(opponent, "Test Mill Plus Four")
        val engine = d.putPermanentOnBattlefield(you, "Test Mill Engine")

        d.submit(
            ActivateAbility(
                you, engine, millEngine.activatedAbilities.first().id,
                costPayment = AdditionalCostPayment(millCount = 12),
            )
        ).error.shouldBeNull()

        d.pendingDecision.shouldBeNull()
        d.isTapped(engine) shouldBe true
        d.getGraveyard(you).size shouldBe 12
    }

    test("a PayCost mill (a morph cost) asks for the order after the payer agrees") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Mill Doubler")
        d.putPermanentOnBattlefield(opponent, "Test Mill Plus Four")
        val morph = d.putCreatureOnBattlefield(you, "Test Mill Morph")
        val morphCost = millMorph.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().single().morphCost
        d.replaceState(d.state.updateEntity(morph) { c ->
            c.with(FaceDownComponent).with(MorphDataComponent(morphCost, "Test Mill Morph"))
        })

        d.submit(TurnFaceUp(you, morph)).error.shouldBeNull()
        val yesNo = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitDecision(you, YesNoResponse(yesNo.id, true)).error.shouldBeNull()

        val decision = d.orderQuestion(you)
        d.getGraveyard(you).size shouldBe 0
        d.choose(decision, "Mill 8 (Test Mill Doubler, then Test Mill Plus Four)")

        d.getGraveyard(you).size shouldBe 8
        d.state.getEntity(morph)?.get<FaceDownComponent>().shouldBeNull()
    }

    test("a mill cost asks the payer for the order before anything is paid") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Mill Doubler")
        d.putPermanentOnBattlefield(opponent, "Test Mill Plus Four")
        val engine = d.putPermanentOnBattlefield(you, "Test Mill Engine")
        val lifeBefore = d.getLifeTotal(you)

        d.submit(ActivateAbility(you, engine, millEngine.activatedAbilities.first().id)).error.shouldBeNull()

        val decision = d.orderQuestion(you)
        withClue("nothing paid yet") {
            d.getGraveyard(you).size shouldBe 0
            d.isTapped(engine) shouldBe false
        }
        d.choose(decision, "Mill 8 (Test Mill Doubler, then Test Mill Plus Four)")
        d.getGraveyard(you).size shouldBe 8
        d.isTapped(engine) shouldBe true
        d.bothPass()
        d.getLifeTotal(you) shouldBe lifeBefore + 1
    }

    test("a mill cost with a lone doubler pays without a question") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Mill Doubler")
        val engine = d.putPermanentOnBattlefield(you, "Test Mill Engine")

        d.submit(ActivateAbility(you, engine, millEngine.activatedAbilities.first().id)).error.shouldBeNull()

        d.getGraveyard(you).size shouldBe 4
        d.isTapped(engine) shouldBe true
    }

    test("a pay-or-suffer mill asks for the order after the payer agrees") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Mill Doubler")
        d.putPermanentOnBattlefield(opponent, "Test Mill Plus Four")
        val lifeBefore = d.getLifeTotal(you)

        d.cast(you, "Mill Or Burn")
        val yesNo = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitDecision(you, YesNoResponse(yesNo.id, true)).error.shouldBeNull()

        d.choose(d.orderQuestion(you), "Mill 12 (Test Mill Plus Four, then Test Mill Doubler)")
        d.pendingDecision.shouldBeNull()
        d.getGraveyard(you).map { d.getCardName(it) }.count { it == "Mountain" } shouldBe 12
        d.getLifeTotal(you) shouldBe lifeBefore
    }

    test("a damage-to-mill replacement doubles with a lone doubler") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mindskinner")
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.giveMana(you, Color.RED, 1)

        d.cast(you, "Lightning Bolt", listOf(opponent))

        d.pendingDecision.shouldBeNull()
        d.getLifeTotal(opponent) shouldBe 20
        d.getGraveyard(opponent).size shouldBe 6
    }

    test("a damage-to-mill replacement asks the milling opponent when the order matters") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Test Mindskinner")
        d.putPermanentOnBattlefield(you, "Test Mill Doubler")
        d.putPermanentOnBattlefield(you, "Test Mill Plus Four")
        d.giveMana(you, Color.RED, 1)

        d.cast(you, "Lightning Bolt", listOf(opponent))

        val decision = d.orderQuestion(opponent)
        decision.options.map { it.substringBefore(" (") } shouldContainExactly listOf("Mill 14", "Mill 10")
        d.choose(decision, decision.options[1])

        d.pendingDecision.shouldBeNull()
        d.getLifeTotal(opponent) shouldBe 20
        d.getGraveyard(opponent).size shouldBe 10
    }
})
