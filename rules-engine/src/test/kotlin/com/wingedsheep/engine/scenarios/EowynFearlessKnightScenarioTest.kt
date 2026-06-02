package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.EowynFearlessKnight
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Éowyn, Fearless Knight (LTR #201) — covers two engine additions:
 *   • [CardPredicate.PowerGreaterThanEntity] — source-relative power target filter.
 *   • [GrantProtectionFromColorsOfEntityEffect] — dynamic per-color protection grant
 *     keyed on the just-exiled creature.
 *
 * Oracle:
 *   Haste
 *   When Éowyn enters, exile target creature an opponent controls with greater power.
 *   Legendary creatures you control gain protection from each of that creature's colors
 *   until end of turn.
 */
class EowynFearlessKnightScenarioTest : FunSpec({

    // CardDefinition.colors derives from manaCost, so the mana cost dictates colour identity.
    fun bear(name: String, mana: String, power: Int, toughness: Int) =
        CardDefinition(
            name = name,
            manaCost = ManaCost.parse(mana),
            typeLine = TypeLine(
                cardTypes = setOf(CardType.CREATURE),
                subtypes = setOf(Subtype("Bear")),
            ),
            creatureStats = CreatureStats(power, toughness),
        )

    fun legendaryKnight(name: String, mana: String, power: Int, toughness: Int) =
        CardDefinition(
            name = name,
            manaCost = ManaCost.parse(mana),
            typeLine = TypeLine(
                supertypes = setOf(Supertype.LEGENDARY),
                cardTypes = setOf(CardType.CREATURE),
                subtypes = setOf(Subtype("Human"), Subtype("Knight")),
            ),
            creatureStats = CreatureStats(power, toughness),
        )

    // 4/4 UB Bear — the legal exile target (power > Éowyn's 3, two colours).
    val DimirBear = bear("Dimir Bear", "{U}{B}", 4, 4)

    // 3/3 red Bear — equal power, NOT a legal target ("greater" is strict).
    val EqualRedBear = bear("Equal Red Bear", "{2}{R}", 3, 3)

    // 2/2 green Bear — lesser power, illegal target.
    val SmallBear = bear("Small Bear", "{1}{G}", 2, 2)

    // 4/4 colorless Eldrazi-style creature — legal target (power > 3) but printed colorless,
    // so the grant should produce zero protection effects.
    val ColorlessBrute = bear("Colorless Brute", "{4}", 4, 4)

    // My 1/1 mono-white legendary — should gain protection from the exiled creature's colours.
    val MyLegend = legendaryKnight("Test Legend", "{W}", 1, 1)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(EowynFearlessKnight, DimirBear, EqualRedBear, SmallBear, ColorlessBrute, MyLegend)
        )
        return driver
    }

    test("ETB exiles target and grants legendary creatures protection from each of its colors") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20,
        )

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls the soon-to-be-exiled 4/4 UB Bear plus a too-small 2/2 Bear.
        val dimirBear = driver.putCreatureOnBattlefield(opponent, "Dimir Bear")
        driver.putCreatureOnBattlefield(opponent, "Small Bear")

        // I control my own legendary + a non-legendary creature to confirm only legends get it.
        val myLegend = driver.putCreatureOnBattlefield(me, "Test Legend")
        val myNonLegend = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        // Cast Éowyn — the ETB trigger pauses for target selection after she enters.
        val eowyn = driver.putCardInHand(me, "Éowyn, Fearless Knight")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.WHITE, 2) // generic {2}
        driver.castSpell(me, eowyn).isSuccess shouldBe true
        driver.bothPass()

        // Target selection: legal targets must contain the 4/4 Dimir Bear and exclude the 2/2.
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legalTargets = (driver.pendingDecision as ChooseTargetsDecision).legalTargets[0] ?: emptyList()
        legalTargets shouldContain dimirBear
        legalTargets.size shouldBe 1 // small bear excluded — strict "greater" power filter

        driver.submitTargetSelection(me, listOf(dimirBear))
        driver.bothPass()

        // Dimir Bear should be exiled (off the battlefield).
        (dimirBear in driver.state.getBattlefield()) shouldBe false

        // My legendary gains protection from each of the exiled creature's colours.
        val projected = StateProjector().project(driver.state)
        projected.hasKeyword(myLegend, "PROTECTION_FROM_BLUE") shouldBe true
        projected.hasKeyword(myLegend, "PROTECTION_FROM_BLACK") shouldBe true
        // No protection from colours it didn't have.
        projected.hasKeyword(myLegend, "PROTECTION_FROM_WHITE") shouldBe false
        projected.hasKeyword(myLegend, "PROTECTION_FROM_RED") shouldBe false

        // My non-legendary creature is unaffected.
        projected.hasKeyword(myNonLegend, "PROTECTION_FROM_BLUE") shouldBe false
        projected.hasKeyword(myNonLegend, "PROTECTION_FROM_BLACK") shouldBe false
    }

    test("source-relative target filter excludes opponent creatures with equal or lesser power") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20,
        )

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a 3/3 (equal power, illegal) and a 4/4 (legal). No 2/2 — keep the legal pool to 1.
        driver.putCreatureOnBattlefield(opponent, "Equal Red Bear")
        val dimirBear = driver.putCreatureOnBattlefield(opponent, "Dimir Bear")

        val eowyn = driver.putCardInHand(me, "Éowyn, Fearless Knight")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, eowyn).isSuccess shouldBe true
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legalTargets = (driver.pendingDecision as ChooseTargetsDecision).legalTargets[0] ?: emptyList()
        legalTargets shouldContain dimirBear
        // The 3/3 is the same power as Éowyn — "greater" is strict, so it must be excluded.
        val equalRedBear = driver.state.getBattlefield().single { entityId ->
            driver.getCardName(entityId) == "Equal Red Bear"
        }
        legalTargets shouldNotContain equalRedBear
    }

    test("colorless target grants no protection (CR 105.2 — colorless is not a color)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20,
        )

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls a 4/4 colorless creature — legal target by power but contributes no colors.
        val brute = driver.putCreatureOnBattlefield(opponent, "Colorless Brute")
        val myLegend = driver.putCreatureOnBattlefield(me, "Test Legend")

        val eowyn = driver.putCardInHand(me, "Éowyn, Fearless Knight")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, eowyn).isSuccess shouldBe true
        driver.bothPass()
        driver.submitTargetSelection(me, listOf(brute))
        driver.bothPass()

        // Brute is exiled.
        (brute in driver.state.getBattlefield()) shouldBe false

        // My legendary gains NO protection — the source projected colorless, so the grant set is empty.
        val projected = StateProjector().project(driver.state)
        Color.entries.forEach { color ->
            projected.hasKeyword(myLegend, "PROTECTION_FROM_${color.name}") shouldBe false
        }
    }

    test("granted protection ends at end of turn (Duration.EndOfTurn)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20,
        )

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dimirBear = driver.putCreatureOnBattlefield(opponent, "Dimir Bear")
        val myLegend = driver.putCreatureOnBattlefield(me, "Test Legend")

        val eowyn = driver.putCardInHand(me, "Éowyn, Fearless Knight")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, eowyn).isSuccess shouldBe true
        driver.bothPass()
        driver.submitTargetSelection(me, listOf(dimirBear))
        driver.bothPass()

        StateProjector().project(driver.state)
            .hasKeyword(myLegend, "PROTECTION_FROM_BLUE") shouldBe true

        // Advance into the cleanup step — end-of-turn effects expire (CR 514.2).
        driver.passPriorityUntil(Step.CLEANUP)

        StateProjector().project(driver.state)
            .hasKeyword(myLegend, "PROTECTION_FROM_BLUE") shouldBe false
        StateProjector().project(driver.state)
            .hasKeyword(myLegend, "PROTECTION_FROM_BLACK") shouldBe false
    }
})
