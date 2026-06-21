package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.FireMagic
import com.wingedsheep.mtg.sets.definitions.fin.cards.IceMagic
import com.wingedsheep.mtg.sets.definitions.fin.cards.RestorationMagic
import com.wingedsheep.mtg.sets.definitions.fin.cards.ThunderMagic
import com.wingedsheep.mtg.sets.definitions.fin.cards.TifasLimitBreak
import com.wingedsheep.mtg.sets.definitions.fin.cards.VincentsLimitBreak
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

/**
 * Tiered (CR 702.183) — "Choose one. As an additional cost to cast this spell, pay the cost
 * associated with that mode." Modeled as a choose-one [com.wingedsheep.sdk.scripting.effects.ModalEffect]
 * with per-tier `additionalManaCost`, reusing the same cast-time per-mode cost machinery as Spree
 * (proven generically by ModalPerModeAdditionalCostTest); these tests pin the mechanic through the
 * six real FIN Tiered cards.
 *
 * Each clause of 702.183a gets a paired assertion:
 * - "Choose one" — exactly one tier may be chosen; choosing two is rejected.
 * - "pay the cost associated with that mode" — the chosen tier's additional cost is added to the
 *   spell's cost (an unaffordable higher tier can't be cast; a payable one can).
 * - the chosen tier's (scaled) effect — and only that tier's — resolves.
 */
class TieredScenarioTest : FunSpec({

    // Vanilla creatures with known toughness so sweep/target damage is deterministic.
    val TwoTwo = CardDefinition.creature(
        name = "Tiered Test 2/2", manaCost = ManaCost.parse("{2}"),
        subtypes = emptySet(), power = 2, toughness = 2, oracleText = ""
    )
    val ThreeThree = CardDefinition.creature(
        name = "Tiered Test 3/3", manaCost = ManaCost.parse("{3}"),
        subtypes = emptySet(), power = 3, toughness = 3, oracleText = ""
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(TwoTwo, ThreeThree))
        d.registerCard(FireMagic)
        d.registerCard(IceMagic)
        d.registerCard(ThunderMagic)
        d.registerCard(RestorationMagic)
        d.registerCard(TifasLimitBreak)
        d.registerCard(VincentsLimitBreak)
        return d
    }

    fun life(d: GameTestDriver, p: EntityId): Int =
        d.state.getEntity(p)!!.get<LifeTotalComponent>()!!.life

    // -------------------------------------------------------------------------
    // Fire Magic — sweep that scales per tier; no targets.
    // -------------------------------------------------------------------------

    test("Fire (tier 0, {0}) costs only {R} and deals 1 to each creature — both survive") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val twoTwo = d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")
        val threeThree = d.putCreatureOnBattlefield(p1, "Tiered Test 3/3")
        d.giveMana(p1, Color.RED, 1) // exactly base {R}, no extra

        val spell = d.putCardInHand(p1, "Fire Magic")
        d.submit(
            CastSpell(p1, spell, chosenModes = listOf(0), paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        d.bothPass()

        // 1 damage to each: both survive.
        d.state.getBattlefield().contains(twoTwo) shouldBe true
        d.state.getBattlefield().contains(threeThree) shouldBe true
    }

    test("Firaga (tier 2, {5}) needs {R} + {5} and deals 3 to each creature — both die") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val twoTwo = d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")
        val threeThree = d.putCreatureOnBattlefield(p1, "Tiered Test 3/3")
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 5) // the additional {5}

        val spell = d.putCardInHand(p1, "Fire Magic")
        d.submit(
            CastSpell(p1, spell, chosenModes = listOf(2), paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        d.bothPass()

        // 3 damage to each: both die.
        d.state.getBattlefield().contains(twoTwo) shouldBe false
        d.state.getBattlefield().contains(threeThree) shouldBe false
    }

    test("Firaga can't be cast without its additional {5} — only base {R} available") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")

        d.giveMana(p1, Color.RED, 1) // base only; {5} unpaid

        val spell = d.putCardInHand(p1, "Fire Magic")
        d.submit(
            CastSpell(p1, spell, chosenModes = listOf(2), paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess.shouldBeFalse()
    }

    test("Tiered is choose-one — submitting two tiers is rejected") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 7)

        val spell = d.putCardInHand(p1, "Fire Magic")
        d.submit(
            CastSpell(p1, spell, chosenModes = listOf(0, 2), paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess.shouldBeFalse()
    }

    // -------------------------------------------------------------------------
    // Thunder Magic — targeted, scaling damage.
    // -------------------------------------------------------------------------

    test("Thundaga (tier 2, {5}{R}) deals 8 to target creature — costs {R}{R}{5}") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        val opp = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val victim = d.putCreatureOnBattlefield(opp, "Tiered Test 3/3")

        d.giveMana(p1, Color.RED, 2)   // base {R} + the {R} in {5}{R}
        d.giveColorlessMana(p1, 5)     // the {5}

        val spell = d.putCardInHand(p1, "Thunder Magic")
        val tgt = ChosenTarget.Permanent(victim)
        d.submit(
            CastSpell(
                p1, spell,
                targets = listOf(tgt),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(listOf(tgt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.bothPass()

        d.state.getBattlefield().contains(victim) shouldBe false
    }

    test("Thunder (tier 0, {0}) deals only 2 — a 3/3 survives") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        val opp = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val victim = d.putCreatureOnBattlefield(opp, "Tiered Test 3/3")

        d.giveMana(p1, Color.RED, 1)

        val spell = d.putCardInHand(p1, "Thunder Magic")
        val tgt = ChosenTarget.Permanent(victim)
        d.submit(
            CastSpell(
                p1, spell,
                targets = listOf(tgt),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(tgt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.bothPass()

        d.state.getBattlefield().contains(victim) shouldBe true
    }

    // -------------------------------------------------------------------------
    // Ice Magic — Blizzard bounce (tier 0).
    // -------------------------------------------------------------------------

    test("Blizzard (tier 0) returns target creature to its owner's hand") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40))
        val p1 = d.activePlayer!!
        val opp = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val victim = d.putCreatureOnBattlefield(opp, "Tiered Test 2/2")

        d.giveColorlessMana(p1, 1)     // the {1} in {1}{U}
        d.giveMana(p1, Color.BLUE, 1)  // the {U}

        val spell = d.putCardInHand(p1, "Ice Magic")
        val tgt = ChosenTarget.Permanent(victim)
        d.submit(
            CastSpell(
                p1, spell,
                targets = listOf(tgt),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(tgt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.bothPass()

        d.state.getBattlefield().contains(victim) shouldBe false
        d.getHand(opp).contains(victim) shouldBe true
    }

    // -------------------------------------------------------------------------
    // Restoration Magic — life gain scales with the tier.
    // -------------------------------------------------------------------------

    test("Curaga (tier 2, {3}{W}) gains 6 life; Cura (tier 1, {1}) gains 3") {
        // Curaga
        run {
            val d = driver()
            d.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val p1 = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")
            d.giveMana(p1, Color.WHITE, 2) // base {W} + the {W} in {3}{W}
            d.giveColorlessMana(p1, 3)     // the {3}
            val before = life(d, p1)
            val spell = d.putCardInHand(p1, "Restoration Magic")
            d.submit(
                CastSpell(p1, spell, chosenModes = listOf(2), paymentStrategy = PaymentStrategy.FromPool)
            ).isSuccess shouldBe true
            d.bothPass()
            life(d, p1) shouldBe (before + 6)
        }
        // Cura
        run {
            val d = driver()
            d.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val p1 = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val perm = d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")
            d.giveMana(p1, Color.WHITE, 1) // base {W}
            d.giveColorlessMana(p1, 1)     // the {1}
            val before = life(d, p1)
            val spell = d.putCardInHand(p1, "Restoration Magic")
            val tgt = ChosenTarget.Permanent(perm)
            d.submit(
                CastSpell(
                    p1, spell,
                    targets = listOf(tgt),
                    chosenModes = listOf(1),
                    modeTargetsOrdered = listOf(listOf(tgt)),
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).isSuccess shouldBe true
            d.bothPass()
            life(d, p1) shouldBe (before + 3)
        }
    }

    // -------------------------------------------------------------------------
    // Tifa's Limit Break — +N/+N, double, triple of target's own P/T.
    // -------------------------------------------------------------------------

    test("Tifa tiers scale a 2/2: Somersault → 4/4, Meteor Strikes → 4/4 (double), Final Heaven → 6/6 (triple)") {
        fun castTifa(modeIndex: Int): Pair<Int?, Int?> {
            val d = driver()
            d.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val p1 = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val creature = d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")
            d.giveMana(p1, Color.GREEN, 2)
            d.giveColorlessMana(p1, 6)
            val spell = d.putCardInHand(p1, "Tifa's Limit Break")
            val tgt = ChosenTarget.Permanent(creature)
            d.submit(
                CastSpell(
                    p1, spell,
                    targets = listOf(tgt),
                    chosenModes = listOf(modeIndex),
                    modeTargetsOrdered = listOf(listOf(tgt)),
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).isSuccess shouldBe true
            d.bothPass()
            val proj = d.state.projectedState
            return proj.getPower(creature) to proj.getToughness(creature)
        }

        castTifa(0) shouldBe (4 to 4) // +2/+2
        castTifa(1) shouldBe (4 to 4) // double 2/2
        castTifa(2) shouldBe (6 to 6) // triple 2/2
    }

    // -------------------------------------------------------------------------
    // Vincent's Limit Break — shared effect, tier picks the base P/T.
    // -------------------------------------------------------------------------

    test("Galian Beast (tier 0) sets target creature you control to base 3/2 and grants dies→return-tapped") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val creature = d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")
        d.giveColorlessMana(p1, 1) // the {1} of {1}{B}
        d.giveMana(p1, Color.BLACK, 1)

        val spell = d.putCardInHand(p1, "Vincent's Limit Break")
        val tgt = ChosenTarget.Permanent(creature)
        d.submit(
            CastSpell(
                p1, spell,
                targets = listOf(tgt),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(tgt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.bothPass()

        val proj = d.state.projectedState
        (proj.getPower(creature) to proj.getToughness(creature)) shouldBe (3 to 2)
    }

    test("the granted dies trigger returns the creature to the battlefield tapped when it dies that turn") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val creature = d.putCreatureOnBattlefield(p1, "Tiered Test 2/2")

        // Galian Beast: base 2/2 → 3/2 + "when this dies, return it tapped".
        d.giveColorlessMana(p1, 1)
        d.giveMana(p1, Color.BLACK, 1)
        val vincent = d.putCardInHand(p1, "Vincent's Limit Break")
        val vTgt = ChosenTarget.Permanent(creature)
        d.submit(
            CastSpell(
                p1, vincent,
                targets = listOf(vTgt),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(vTgt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.bothPass()

        // Kill it: Thunder (tier 0) deals 2 — lethal to the now-3/2 creature.
        d.giveMana(p1, Color.RED, 1)
        val thunder = d.putCardInHand(p1, "Thunder Magic")
        val tTgt = ChosenTarget.Permanent(creature)
        d.submit(
            CastSpell(
                p1, thunder,
                targets = listOf(tTgt),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(tTgt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.bothPass() // resolve Thunder → creature dies
        d.bothPass() // resolve the granted dies trigger → return it tapped

        // The creature died and re-entered the battlefield tapped under its owner's control.
        val returned = d.findPermanent(p1, "Tiered Test 2/2")
        (returned != null) shouldBe true
        d.isTapped(returned!!) shouldBe true
        d.getGraveyardCardNames(p1).contains("Tiered Test 2/2") shouldBe false
    }
})
