package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.wasKickedChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Kicker [A] and/or [B]" — two independently payable kicker costs (CR 702.33).
 *
 * - 702.33b: "Kicker [cost 1] and/or [cost 2]" means two kicker abilities — each may be paid or not.
 * - 702.33d: paying *any* kicker cost makes the spell kicked.
 * - 702.33f: "if it was kicked with its [A] kicker" / "…[B] kicker" abilities are each linked to the
 *   first / second listed kicker cost.
 *
 * Cards declare the two costs as two `KeywordAbility.kicker(...)` entries; a cast picks among them
 * with `CastSpell.declaredCostIndices`. Tests cover each combination's cost, the linked reads in all
 * three contexts (a spell's own effect, a permanent's enters trigger, a "when you cast" trigger is
 * covered by Wastescape Battlemage's scenario test), the shared "kicked" fact, and validation.
 */
class LinkedKickerCostsScenarioTest : ScenarioTestBase() {

    // Instant shape (Invasion's "and/or" instants): the riders read the declaration off the spell.
    private val twinKickerBolt = card("Twin Kicker Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Kicker {1} and/or {W}\nYou gain 1 life. If this spell was kicked with its {1} " +
            "kicker, you gain 2 life. If it was kicked with its {W} kicker, you gain 4 life."
        keywordAbility(KeywordAbility.kicker("{1}"))
        keywordAbility(KeywordAbility.kicker("{W}"))
        spell {
            effect = Effects.GainLife(1) then
                Effects.If(condition = Conditions.WasKickedWithFirstKicker, then = Effects.GainLife(2)) then
                Effects.If(condition = Conditions.WasKickedWithSecondKicker, then = Effects.GainLife(4))
        }
    }

    // Thornscape Battlemage shape: the linked abilities are enters triggers on the permanent.
    private val twinKickerMage = card("Twin Kicker Mage") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Wizard"
        power = 1
        toughness = 1
        oracleText = "Kicker {R} and/or {W}\nWhen this creature enters, if it was kicked with its " +
            "{R} kicker, you gain 2 life.\nWhen this creature enters, if it was kicked with its {W} " +
            "kicker, you gain 4 life."
        keywordAbility(KeywordAbility.kicker("{R}"))
        keywordAbility(KeywordAbility.kicker("{W}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.WasKickedWithFirstKicker
            effect = Effects.GainLife(2)
        }
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.WasKickedWithSecondKicker
            effect = Effects.GainLife(4)
        }
    }

    // A single-kicker control: no per-kicker identity is recorded.
    private val singleKickerBear = card("Single Kicker Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.kicker("{1}"))
    }

    // "Whenever you cast a kicked spell" — CR 702.33d: either kicker kicks the spell.
    private val kickedWatcher = card("Linked Kicked Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
            effect = Effects.GainLife(10)
        }
    }

    init {
        cardRegistry.register(twinKickerBolt)
        cardRegistry.register(twinKickerMage)
        cardRegistry.register(singleKickerBear)
        cardRegistry.register(kickedWatcher)

        fun board(card: String, mountains: Int = 1, plains: Int = 1, forests: Int = 1) = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, card)
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withLandsOnBattlefield(1, "Plains", plains)
            .withLandsOnBattlefield(1, "Forest", forests)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.cast(card: String, indices: Set<Int>?) = execute(
            CastSpell(
                player1Id, findCardsInHand(1, card).single(),
                declaredCostSlot = if (indices == null) null else ChoiceSlot.KICKED,
                declaredCostIndices = indices ?: emptySet(),
            )
        )

        fun TestGame.untappedLands() = state.getBattlefield().count { id ->
            state.getEntity(id)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() == false &&
                state.projectedState.hasType(id, "LAND")
        }

        // -----------------------------------------------------------------------------------
        // CR 702.33b / 702.33f — spell rider, each combination; cost charged per declared kicker
        // -----------------------------------------------------------------------------------

        listOf(
            null to (1 to 1),          // unkicked: {R}, gain 1
            setOf(0) to (3 to 2),      // first kicker {1}: {1}{R}, gain 1 + 2
            setOf(1) to (5 to 2),      // second kicker {W}: {R}{W}, gain 1 + 4
            setOf(0, 1) to (7 to 3),   // both: {1}{R}{W}, gain 1 + 2 + 4
        ).forEach { (indices, expected) ->
            val (gained, landsSpent) = expected
            test("spell rider reads each linked kicker independently — declared $indices") {
                val game = board("Twin Kicker Bolt")
                game.cast("Twin Kicker Bolt", indices).error shouldBe null
                game.untappedLands() shouldBe 3 - landsSpent
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 20 + gained
            }
        }

        test("kicking with both is unaffordable without mana for both kicker costs") {
            // {R} + {1} + {W} needs three lands; only Mountain and Plains are here.
            val game = board("Twin Kicker Bolt", forests = 0)
            game.cast("Twin Kicker Bolt", setOf(0, 1)).error shouldNotBe null
            game.cast("Twin Kicker Bolt", setOf(1)).error shouldBe null
        }

        test("an empty index set on a two-kicker card declares every kicker") {
            val game = board("Twin Kicker Bolt")
            game.cast("Twin Kicker Bolt", emptySet()).error shouldBe null
            game.untappedLands() shouldBe 0
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 27
        }

        test("an index naming a kicker the card does not list is rejected") {
            val game = board("Twin Kicker Bolt")
            game.cast("Twin Kicker Bolt", setOf(2)).error shouldNotBe null
        }

        // -----------------------------------------------------------------------------------
        // CR 702.33f on a permanent — the linked enters triggers read the durable stamp
        // -----------------------------------------------------------------------------------

        test("enters triggers: only the paid kicker's trigger fires, and the permanent is kicked") {
            val game = board("Twin Kicker Mage")
            game.cast("Twin Kicker Mage", setOf(1)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 24
            val mage = game.state.getEntity(game.findPermanent("Twin Kicker Mage").shouldNotBeNull()).shouldNotBeNull()
            mage.wasKickedChoice() shouldBe true
            val chosen = mage.get<CastChoicesComponent>().shouldNotBeNull().chosen
            chosen.containsKey(ChoiceSlot.FIRST_KICKER) shouldBe false
            chosen.containsKey(ChoiceSlot.SECOND_KICKER) shouldBe true
        }

        test("enters triggers: both kickers paid fire both linked triggers") {
            val game = board("Twin Kicker Mage")
            game.cast("Twin Kicker Mage", setOf(0, 1)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 26
        }

        test("enters triggers: unkicked fires neither") {
            val game = board("Twin Kicker Mage")
            game.cast("Twin Kicker Mage", null).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.state.getEntity(game.findPermanent("Twin Kicker Mage")!!)!!.wasKickedChoice() shouldBe false
        }

        // -----------------------------------------------------------------------------------
        // CR 702.33d — paying either kicker makes the spell kicked
        // -----------------------------------------------------------------------------------

        test("a kicked-spell payoff sees a spell kicked with only its second kicker") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Twin Kicker Mage")
                .withCardOnBattlefield(1, "Linked Kicked Watcher")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.cast("Twin Kicker Mage", setOf(1)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 34
        }

        test("a single-kicker card records no per-kicker identity") {
            val game = board("Single Kicker Bear")
            game.cast("Single Kicker Bear", emptySet()).error shouldBe null
            game.resolveStack()
            val bear = game.state.getEntity(game.findPermanent("Single Kicker Bear")!!)!!
            bear.wasKickedChoice() shouldBe true
            val chosen = bear.get<CastChoicesComponent>().shouldNotBeNull().chosen
            chosen.containsKey(ChoiceSlot.FIRST_KICKER) shouldBe false
            chosen.containsKey(ChoiceSlot.SECOND_KICKER) shouldBe false
        }

        test("the enumerator offers each kicker combination as its own cast") {
            val game = board("Twin Kicker Bolt")
            val kicked = game.getLegalActions(1).filter { it.actionType == "CastWithKicker" }
            kicked.map { (it.action as CastSpell).declaredCostIndices } shouldBe listOf(setOf(0), setOf(1), setOf(0, 1))
            kicked.map { it.description } shouldBe listOf(
                "Cast Twin Kicker Bolt (Kicked {1})",
                "Cast Twin Kicker Bolt (Kicked {W})",
                "Cast Twin Kicker Bolt (Kicked {1} + {W})",
            )
        }
    }
}
