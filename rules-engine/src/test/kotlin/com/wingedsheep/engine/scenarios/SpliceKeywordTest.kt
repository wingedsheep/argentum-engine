package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Splice onto [quality] (CR 702.47, Champions of Kamigawa).
 *
 * 702.47a "Splice onto [quality] [cost]" means "You may reveal this card from your hand as you cast a
 *   [quality] spell. If you do, that spell gains the text of this card's rules text and you pay [cost]
 *   as an additional cost to cast that spell." Paying it follows the additional-cost rules
 *   (601.2b, 601.2f–h). The spliced card *stays in hand* — it is only revealed.
 * 702.47b You can't use a splice ability if you can't make the required choices (targets, etc.) for
 *   that card's rules text, and you can't splice one card onto the same spell more than once. With
 *   several cards spliced on, the effects of the main spell must happen first.
 * 702.47c The spell has the main spell's characteristics *plus the rules text* of each spliced card —
 *   no name, mana cost, colour, or types come along. The CR's own example: a red Glacial Ray spliced
 *   onto blue Reach Through Mists is still a blue spell, so its damage can be dealt to a creature with
 *   protection from red.
 * 702.47d Targets for the added text are chosen normally (601.2c).
 * 702.47e The spell loses any splice changes once it leaves the stack.
 *
 * Exercised with inline cards so the engine behaviour is pinned independently of the printed Kamigawa
 * splice cycle; Through the Breach has its own scenario test.
 */
class SpliceKeywordTest : FunSpec({

    // ---- Arcane spells to splice onto -------------------------------------------------------

    // {R} Instant — Arcane, "deals 1 damage to target player".
    val arcaneBolt = card("Test Arcane Bolt") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant — Arcane"
        spell {
            val t = target("bolt", Targets.Player)
            effect = Effects.DealDamage(1, t)
        }
    }

    // A *blue* Arcane spell with no targets of its own — the Reach Through Mists role in CR 702.47c.
    val arcaneMeditation = card("Test Arcane Meditation") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Instant — Arcane"
        spell { effect = Effects.DrawCards(1) }
    }

    // Same effect, no Arcane subtype — nothing may be spliced onto it.
    val plainBolt = card("Test Plain Bolt") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        spell {
            val t = target("bolt", Targets.Player)
            effect = Effects.DealDamage(1, t)
        }
    }

    // ---- Splice cards ------------------------------------------------------------------------

    // The Glacial Ray shape: a red splice card whose own text targets a creature.
    val spliceRay = card("Test Splice Ray") {
        manaCost = "{1}{R}"
        colorIdentity = "R"
        typeLine = "Instant — Arcane"
        splice("{1}{R}")
        spell {
            val t = target("ray", Targets.Creature)
            effect = Effects.DealDamage(2, t)
        }
    }

    // An untargeted splice card, so ordering and cost can be asserted without target plumbing.
    val spliceGain = card("Test Splice Gain") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant — Arcane"
        splice("{1}")
        spell { effect = Effects.GainLife(3) }
    }

    // A 2/2 with protection from red — the target that proves CR 702.47c.
    val redWardedBear = card("Test Red Warded Bear") {
        manaCost = "{2}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))
    }

    val spliceTestCards = listOf(
        arcaneBolt, arcaneMeditation, plainBolt, spliceRay, spliceGain, redWardedBear
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(spliceTestCards)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun spliceCast(
        player: EntityId,
        cardId: EntityId,
        spliced: List<EntityId>,
        targets: List<ChosenTarget> = emptyList(),
    ) = CastSpell(
        playerId = player,
        cardId = cardId,
        targets = targets,
        splicedCardIds = spliced,
        paymentStrategy = PaymentStrategy.FromPool,
    )

    // ---- 702.47a — the cost, and the card staying in hand ------------------------------------

    test("702.47a: the splice cost is charged as an additional cost and the card stays in hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        // {R} for the bolt + {1} for the splice = exactly two mana.
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            spliceCast(player, bolt, listOf(gain), listOf(ChosenTarget.Player(opponent)))
        ).error shouldBe null
        driver.bothPass()

        // Both halves happened: the bolt's damage and the spliced life gain.
        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(player) shouldBe 23

        // 702.47a — the spliced card was only *revealed*; it never left hand.
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain gain
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldNotBe listOf(gain)
    }

    test("702.47a: a cast that can't pay the splice cost on top of the spell's own is rejected") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        // Only {R} — enough for the bolt alone, one short of the {1} splice cost.
        driver.giveMana(player, Color.RED, 1)

        driver.submitExpectFailure(
            spliceCast(player, bolt, listOf(gain), listOf(ChosenTarget.Player(opponent)))
        )
        // Nothing resolved, and the bolt is still castable on its own.
        driver.getLifeTotal(opponent) shouldBe 20
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain bolt
    }

    test("702.47a: the spliced card is still castable later — it was never spent") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            spliceCast(player, bolt, listOf(gain), listOf(ChosenTarget.Player(opponent)))
        ).error shouldBe null
        driver.bothPass()

        // Now cast the same card normally for its own {R}.
        driver.giveMana(player, Color.RED, 1)
        driver.submit(CastSpell(player, gain, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()

        // 3 life from the splice, 3 more from casting it: 20 + 3 + 3.
        driver.getLifeTotal(player) shouldBe 26
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain gain
    }

    // ---- 702.47b — no double-splicing, and ordering ------------------------------------------

    test("702.47b: the same card cannot be spliced onto one spell twice") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 5)

        driver.submitExpectFailure(
            spliceCast(player, bolt, listOf(gain, gain), listOf(ChosenTarget.Player(opponent)))
        )
        driver.getLifeTotal(player) shouldBe 20
    }

    test("702.47b: the main spell's effects happen before the spliced text") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            spliceCast(player, bolt, listOf(gain), listOf(ChosenTarget.Player(opponent)))
        ).error shouldBe null
        val resolution = driver.bothPass()

        // The bolt's damage must be emitted before the spliced life gain.
        val damageIndex = resolution.events.indexOfFirst { it is DamageDealtEvent }
        val gainIndex = resolution.events.indexOfFirst {
            it is LifeChangedEvent && it.playerId == player && it.newLife > it.oldLife
        }
        damageIndex shouldBeGreaterThan -1
        gainIndex shouldBeGreaterThan -1
        damageIndex shouldBeLessThan gainIndex
    }

    test("702.47b: two cards spliced onto one spell both resolve, in the declared order") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val ray = driver.putCardInHand(player, "Test Splice Ray")
        // {R} bolt + {1} gain + {1}{R} ray = four mana, two of them red.
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            spliceCast(
                player, bolt, listOf(gain, ray),
                // Main spell's target first, then each spliced card's, in splice order.
                listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(bear))
            )
        ).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 19       // the bolt
        driver.getLifeTotal(player) shouldBe 23         // the spliced life gain
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)) shouldContain bear  // the spliced ray
    }

    // ---- 702.47c — characteristics are not gained --------------------------------------------

    test("702.47c: the spell keeps its own colour — a red splice on a blue spell is still blue") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val meditation = driver.putCardInHand(player, "Test Arcane Meditation")
        val ray = driver.putCardInHand(player, "Test Splice Ray")
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            spliceCast(player, meditation, listOf(ray), listOf(ChosenTarget.Permanent(bear)))
        ).error shouldBe null

        // While it is on the stack the spell is still mono-blue: it gained text, not colour.
        val spellId = driver.getTopOfStack().shouldNotBeNull()
        val spellCard = driver.state.getEntity(spellId)?.get<CardComponent>().shouldNotBeNull()
        spellCard.colors shouldBe setOf(Color.BLUE)
        spellCard.name shouldBe "Test Arcane Meditation"
        // The splice is recorded on the stack object, not on the card's characteristics.
        driver.state.getEntity(spellId)?.get<SpellOnStackComponent>()
            ?.splicedCardNames shouldBe listOf("Test Splice Ray")
    }

    test("702.47c: spliced damage from a red card can hit a creature with protection from red") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val meditation = driver.putCardInHand(player, "Test Arcane Meditation")
        val ray = driver.putCardInHand(player, "Test Splice Ray")
        // Protection from red would stop Test Splice Ray cast on its own; spliced onto a blue spell
        // the damage comes from that blue spell instead (the CR 702.47c Glacial Ray example).
        val warded = driver.putCreatureOnBattlefield(opponent, "Test Red Warded Bear")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            spliceCast(player, meditation, listOf(ray), listOf(ChosenTarget.Permanent(warded)))
        ).error shouldBe null
        driver.bothPass()

        // 2 damage to a 2/2 — it dies.
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)) shouldContain warded
    }

    // ---- 702.47d — targets for the added text ------------------------------------------------

    test("702.47d: the spliced text gets its own target, separate from the main spell's") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val ray = driver.putCardInHand(player, "Test Splice Ray")
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.RED, 4)

        driver.submit(
            spliceCast(
                player, bolt, listOf(ray),
                listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(bear))
            )
        ).error shouldBe null
        driver.bothPass()

        // The bolt hit the player, the spliced ray hit the creature — neither stole the other's target.
        driver.getLifeTotal(opponent) shouldBe 19
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)) shouldContain bear
    }

    // ---- 702.47e — splice is lost when the spell leaves the stack ----------------------------

    test("702.47e: countering the spell loses the splice too — neither half resolves") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            spliceCast(player, bolt, listOf(gain), listOf(ChosenTarget.Player(opponent)))
        ).error shouldBe null

        val spellId = driver.getTopOfStack().shouldNotBeNull()
        // The caster keeps priority after casting (CR 117.3c), so hand it over before responding.
        driver.passPriority(player)

        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(spellId)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        // Neither the main spell's damage nor the spliced life gain happened.
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(player) shouldBe 20
        // The spliced card is still in hand — the splice cost bought nothing, but the card is intact.
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain gain
    }

    // ---- Cast-time legality: what the enumerator offers --------------------------------------

    test("a CastWithSplice variant is offered for an Arcane spell with a splice card in hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Test Arcane Bolt")
        driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 4)

        val spliceActions = driver.legalActions(player).filter { it.actionType == "CastWithSplice" }
        spliceActions.map { it.description } shouldContain "Cast Test Arcane Bolt (Splice Test Splice Gain)"
        // The plain cast survives alongside it — splice is optional (CR 702.47a "you *may* reveal").
        driver.legalActions(player).any {
            it.actionType == "CastSpell" && it.description == "Cast Test Arcane Bolt"
        } shouldBe true
    }

    test("no splice variant is offered onto a spell that lacks the quality") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Test Plain Bolt")
        driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 4)

        driver.legalActions(player).none {
            it.actionType == "CastWithSplice" && it.description.contains("Test Plain Bolt")
        } shouldBe true
    }

    test("702.47a: splicing onto a spell that lacks the quality is rejected even if requested") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val plain = driver.putCardInHand(player, "Test Plain Bolt")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 4)

        driver.submitExpectFailure(
            spliceCast(player, plain, listOf(gain), listOf(ChosenTarget.Player(opponent)))
        )
        driver.getLifeTotal(player) shouldBe 20
    }

    test("702.47b: no splice variant when the spliced text has no legal target") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Test Arcane Bolt")
        driver.putCardInHand(player, "Test Splice Ray")
        driver.giveMana(player, Color.RED, 5)

        // Test Splice Ray needs a creature and the battlefield is empty of them, so CR 702.47b
        // forbids choosing to splice at all.
        driver.legalActions(player).none { it.actionType == "CastWithSplice" } shouldBe true
    }

    test("a card in hand without splice cannot be spliced") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.giveMana(player, Color.RED, 4)

        driver.submitExpectFailure(
            spliceCast(player, bolt, listOf(bears), listOf(ChosenTarget.Player(opponent)))
        )
    }

    test("a splice card that is not in hand cannot be spliced") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInHand(player, "Test Arcane Bolt")
        val gainInGraveyard = driver.putCardInGraveyard(player, "Test Splice Gain")
        driver.giveMana(player, Color.RED, 4)

        driver.submitExpectFailure(
            spliceCast(player, bolt, listOf(gainInGraveyard), listOf(ChosenTarget.Player(opponent)))
        )
    }
})
