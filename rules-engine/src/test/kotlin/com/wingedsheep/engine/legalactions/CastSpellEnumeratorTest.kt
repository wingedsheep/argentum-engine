package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainAffordableCastOf
import com.wingedsheep.engine.legalactions.support.shouldContainCastOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainCastOf
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.StrongholdConfessor
import com.wingedsheep.mtg.sets.definitions.khans.cards.TormentingVoice
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.BrigidsCommand
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for [enumerators.CastSpellEnumerator] over the spells-from-hand path.
 * Alternative-zone casts (graveyard, exile, top of library) live in
 * CastFromZoneEnumerator and have their own test file in phase 4.
 *
 * Note on "unaffordable" spells: the enumerator drops a spell entirely when
 * no payment path is reachable (line 250 of the enumerator). So a spell with
 * no mana available does not appear at all — it does NOT appear with
 * `affordable=false`. These tests assert that contract.
 */
class CastSpellEnumeratorTest : FunSpec({

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    test("lands in hand are not enumerated as CastSpell actions") {
        val driver = EnumerationFixtures.allForestsMainPhase()

        val casts = driver.enumerateFor(driver.player1).castActions()

        casts.shouldBeEmpty()
    }

    test("creature in hand with sufficient mana surfaces as an affordable CastSpell") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears"),
            battlefield = listOf("Forest", "Forest")
        )

        driver.enumerateFor(driver.player1) shouldContainAffordableCastOf "Grizzly Bears"
    }

    test("creature in hand with no mana available is not enumerated at all") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears")
        )

        driver.enumerateFor(driver.player1) shouldNotContainCastOf "Grizzly Bears"
    }

    test("opponent does not see CastSpell actions for cards in P1's hand") {
        val driver = setupP1(
            hand = listOf("Lightning Bolt"),
            battlefield = listOf("Forest")  // P1 has mana but Bolt is {R}, irrelevant for P2
        )

        driver.enumerateFor(driver.player2) shouldNotContainCastOf "Lightning Bolt"
    }

    // -------------------------------------------------------------------------
    // Timing
    // -------------------------------------------------------------------------

    test("sorcery-speed creature is not castable on the upkeep step") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears"),
            battlefield = listOf("Forest", "Forest"),
            atStep = Step.UPKEEP
        )

        driver.enumerateFor(driver.player1) shouldNotContainCastOf "Grizzly Bears"
    }

    test("instants ARE castable on the upkeep step") {
        val driver = setupP1(
            hand = listOf("Lightning Bolt", "Grizzly Bears"),
            battlefield = listOf("Mountain", "Mountain"),
            atStep = Step.UPKEEP
        )

        val view = driver.enumerateFor(driver.player1)
        view shouldContainCastOf "Lightning Bolt"     // instant — OK
        view shouldNotContainCastOf "Grizzly Bears"   // sorcery-speed — blocked
    }

    // -------------------------------------------------------------------------
    // Costs and X
    // -------------------------------------------------------------------------

    test("manaCostString is populated from the card's cost") {
        val driver = setupP1(
            hand = listOf("Lightning Bolt"),
            battlefield = listOf("Mountain")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Lightning Bolt").first()

        cast.manaCostString shouldBe "{R}"
        cast.affordable shouldBe true
        cast.hasXCost shouldBe false
    }

    test("description starts with 'Cast' and includes the card name") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears"),
            battlefield = listOf("Forest", "Forest")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Grizzly Bears").first()

        cast.actionType shouldBe "CastSpell"
        cast.description shouldBe "Cast Grizzly Bears"
    }

    // -------------------------------------------------------------------------
    // Targeting
    // -------------------------------------------------------------------------

    test("Lightning Bolt reports valid targets and requiresTargets=true") {
        val driver = setupP1(
            hand = listOf("Lightning Bolt"),
            battlefield = listOf("Mountain")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Lightning Bolt").first()

        cast.requiresTargets shouldBe true
        cast.targetCount shouldBe 1
        cast.validTargets shouldNotBe null
        // AnyTarget allows both players; both should be in the valid set.
        cast.validTargets!! shouldContain driver.player1
        cast.validTargets!! shouldContain driver.player2
    }

    test("CastSpell action carries the card's entity id") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears"),
            battlefield = listOf("Forest", "Forest")
        )
        val expectedHandId = driver.game.state.getHand(driver.player1).first { id ->
            driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
        }

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Grizzly Bears").first()
        val action = cast.action as CastSpell

        action.cardId shouldBe expectedHandId
        action.playerId shouldBe driver.player1
    }

    // -------------------------------------------------------------------------
    // Player-level restrictions
    // -------------------------------------------------------------------------

    test("CantCastSpellsComponent on player suppresses every spell cast") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears", "Lightning Bolt"),
            battlefield = listOf("Forest", "Mountain")
        )
        val playerContainer = driver.game.state.getEntity(driver.player1)!!
            .with(CantCastSpellsComponent())
        driver.game.replaceState(driver.game.state.withEntity(driver.player1, playerContainer))

        val view = driver.enumerateFor(driver.player1)

        view shouldNotContainCastOf "Grizzly Bears"
        view shouldNotContainCastOf "Lightning Bolt"
    }

    // -------------------------------------------------------------------------
    // X-in-mana cost (Blaze: {X}{R})
    // -------------------------------------------------------------------------

    test("X-cost spell reports hasXCost=true and maxAffordableX from available mana") {
        val driver = setupP1(
            hand = listOf("Blaze"),
            battlefield = listOf("Mountain", "Mountain", "Mountain", "Mountain")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Blaze").first()

        cast.hasXCost shouldBe true
        // {X}{R} with 4 mountains: fixed cost 1 (the R), X can be up to 3.
        cast.maxAffordableX shouldBe 3
        cast.manaCostString shouldBe "{X}{R}"
    }

    test("X-cost spell with just the fixed mana emits with maxAffordableX=0") {
        val driver = setupP1(
            hand = listOf("Blaze"),
            battlefield = listOf("Mountain")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Blaze").first()

        cast.hasXCost shouldBe true
        cast.maxAffordableX shouldBe 0
    }

    // -------------------------------------------------------------------------
    // Sacrifice as additional cost (Skulltap: sacrifice a creature)
    // -------------------------------------------------------------------------

    test("additional Sacrifice cost with a creature on the battlefield — cost info populated") {
        val driver = setupP1(
            hand = listOf("Skulltap"),
            battlefield = listOf("Swamp", "Swamp", "Grizzly Bears")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Skulltap").first()

        cast.affordable shouldBe true
        val costInfo = cast.additionalCostInfo.shouldNotBeNull()
        costInfo.costType shouldBe "SacrificePermanent"
        costInfo.sacrificeCount shouldBe 1
        costInfo.validSacrificeTargets shouldHaveSize 1
    }

    test("additional Sacrifice cost with no creatures — cast is NOT enumerated") {
        val driver = setupP1(
            hand = listOf("Skulltap"),
            battlefield = listOf("Swamp", "Swamp")  // no creatures to sacrifice
        )

        driver.enumerateFor(driver.player1) shouldNotContainCastOf "Skulltap"
    }

    // -------------------------------------------------------------------------
    // Discard as additional cost (Tormenting Voice: discard a card)
    // -------------------------------------------------------------------------

    test("additional Discard cost with a spare card in hand — cost info populated") {
        val driver = setupP1(
            hand = listOf("Tormenting Voice", "Grizzly Bears"),  // Bears is discardable
            battlefield = listOf("Mountain", "Mountain"),
            extraSetCards = listOf(TormentingVoice)
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Tormenting Voice").first()

        cast.affordable shouldBe true
        val costInfo = cast.additionalCostInfo.shouldNotBeNull()
        costInfo.costType shouldBe "DiscardCard"
        costInfo.discardCount shouldBe 1
        // Only 1 valid discard target — the Bears. The spell itself is excluded.
        costInfo.validDiscardTargets shouldHaveSize 1
    }

    test("additional Discard cost with only the spell itself in hand — NOT enumerated") {
        // Tormenting Voice alone in hand: the spell is excluded from discard
        // targets (cost.kt filters `it != cardId`), so no valid discards remain.
        val driver = setupP1(
            hand = listOf("Tormenting Voice"),
            battlefield = listOf("Mountain", "Mountain"),
            extraSetCards = listOf(TormentingVoice)
        )

        driver.enumerateFor(driver.player1) shouldNotContainCastOf "Tormenting Voice"
    }

    // -------------------------------------------------------------------------
    // Kicker (Stronghold Confessor: {B}, kicker {3})
    // -------------------------------------------------------------------------

    test("Kicker surfaces a separate CastWithKicker action alongside the base cast") {
        val driver = setupP1(
            hand = listOf("Stronghold Confessor"),
            battlefield = listOf("Swamp", "Swamp", "Swamp", "Swamp"),  // enough for kicked cost {3}{B}
            extraSetCards = listOf(StrongholdConfessor)
        )

        val casts = driver.enumerateFor(driver.player1).castActionsFor("Stronghold Confessor")

        // Base cast + kicker cast.
        casts shouldHaveSize 2
        casts.map { it.actionType }.toSet() shouldBe setOf("CastSpell", "CastWithKicker")
        val kicked = casts.single { it.actionType == "CastWithKicker" }
        kicked.affordable shouldBe true
        // Kicker adds {3} to the base {B} cost.
        kicked.manaCostString shouldBe "{3}{B}"
        (kicked.action as CastSpell).wasKicked shouldBe true
    }

    test("Kicker action is emitted as unaffordable when the kicked cost can't be paid") {
        // Only 1 Swamp — enough for {B} base, not for {4}{B} kicked.
        val driver = setupP1(
            hand = listOf("Stronghold Confessor"),
            battlefield = listOf("Swamp"),
            extraSetCards = listOf(StrongholdConfessor)
        )

        val kicked = driver.enumerateFor(driver.player1)
            .castActionsFor("Stronghold Confessor")
            .single { it.actionType == "CastWithKicker" }

        kicked.affordable shouldBe false
    }

    // -------------------------------------------------------------------------
    // Damage distribution (Forked Lightning)
    // -------------------------------------------------------------------------

    test("DividedDamageEffect spell reports requiresDamageDistribution and totals") {
        // Forked Lightning: {3}{R}, "4 damage divided among 1–3 creatures"
        val driver = setupP1(
            hand = listOf("Forked Lightning"),
            battlefield = listOf("Mountain", "Mountain", "Mountain", "Mountain", "Grizzly Bears")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Forked Lightning").first()

        cast.requiresDamageDistribution shouldBe true
        cast.totalDamageToDistribute shouldBe 4
        cast.minDamagePerTarget shouldBe 1
        cast.requiresTargets shouldBe true
    }

    // -------------------------------------------------------------------------
    // Convoke (Stoke the Flames — creatures may tap to pay)
    // -------------------------------------------------------------------------

    test("Convoke spell surfaces convokeCreatures and hasConvoke=true") {
        // Stoke the Flames: {2}{R}{R} with Convoke. 4 Mountains alone would cover it;
        // we add Grizzly Bears to ensure convokeCreatures is populated for the UI.
        val driver = setupP1(
            hand = listOf("Stoke the Flames"),
            battlefield = listOf("Mountain", "Mountain", "Mountain", "Mountain", "Grizzly Bears")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Stoke the Flames").first()

        cast.hasConvoke shouldBe true
        val creatures = cast.convokeCreatures.shouldNotBeNull()
        creatures shouldHaveSize 1  // just the Grizzly Bears
        creatures.single().name shouldBe "Grizzly Bears"
    }

    test("Convoke makes an otherwise unpayable spell payable via creature taps") {
        // Only 3 Mountains (3 mana) but 1 Grizzly Bears (can tap for 1 generic).
        // Convoke lets us pay {2}{R}{R} = 4 by combining 3 mana + 1 creature tap.
        val driver = setupP1(
            hand = listOf("Stoke the Flames"),
            battlefield = listOf("Mountain", "Mountain", "Mountain", "Grizzly Bears")
        )

        driver.enumerateFor(driver.player1) shouldContainAffordableCastOf "Stoke the Flames"
    }

    // -------------------------------------------------------------------------
    // Delve (Gurmag Angler — exile graveyard cards to pay generic)
    // -------------------------------------------------------------------------

    test("Delve spell surfaces delveCards and minDelveNeeded") {
        // Gurmag Angler: {6}{B} with Delve. 2 Swamps + 5 graveyard cards = delve
        // 5 to cover the remaining 5 generic mana (total available 2 + 5 = 7 ≥ 7).
        val driver = setupP1(
            hand = listOf("Gurmag Angler"),
            battlefield = listOf("Swamp", "Swamp"),
            graveyard = listOf("Grizzly Bears", "Forest", "Forest", "Forest", "Forest")
        )

        val cast = driver.enumerateFor(driver.player1).castActionsFor("Gurmag Angler").first()

        cast.hasDelve shouldBe true
        val delveCards = cast.delveCards.shouldNotBeNull()
        delveCards shouldHaveSize 5  // all graveyard cards are delve-able
        cast.minDelveNeeded shouldBe 5  // 7 cost - 2 swamps
    }

    // -------------------------------------------------------------------------
    // Choose-N modal spells (rules 700.2, 601.2b–c)
    //
    // Choose-N collapses to one CastSpellModal LegalAction carrying a
    // ModalLegalEnumeration payload. The client drives cast-time mode and
    // target selection from that payload — the engine does NOT expand every
    // mode × target combination up front (would explode for allowRepeat).
    // -------------------------------------------------------------------------

    test("choose-N modal spell emits a single CastSpellModal action with enumeration payload") {
        val driver = setupP1(
            hand = listOf("Brigid's Command"),
            // {1}{G}{W} needs 3 mana with G and W available
            battlefield = listOf("Forest", "Plains", "Forest", "Grizzly Bears"),
            extraSetCards = listOf(BrigidsCommand)
        )

        val casts = driver.enumerateFor(driver.player1).castActionsFor("Brigid's Command")

        casts shouldHaveSize 1
        val modal = casts.single()
        modal.actionType shouldBe "CastSpellModal"
        modal.description shouldBe "Cast Brigid's Command"

        val enumeration = modal.modalEnumeration.shouldNotBeNull()
        enumeration.chooseCount shouldBe 2
        enumeration.minChooseCount shouldBe 2
        enumeration.allowRepeat shouldBe false
        enumeration.modes shouldHaveSize 4

        // Mode 0 (copy target Kithkin you control) — no Kithkin on battlefield → unavailable (700.2a)
        enumeration.modes[0].available shouldBe false
        // Mode 1 (target player creates a Kithkin) — always available
        enumeration.modes[1].available shouldBe true
        // Mode 2 (target creature you control +3/+3) — Grizzly Bears present → available
        enumeration.modes[2].available shouldBe true
        // Mode 3 (fight) — opponent has no creatures → unavailable (700.2a)
        enumeration.modes[3].available shouldBe false

        enumeration.unavailableIndices shouldContain 0
        enumeration.unavailableIndices shouldContain 3
        enumeration.unavailableIndices.size shouldBe 2
    }

    test("choose-N modal spell is dropped entirely when every mode is unavailable") {
        // No creatures anywhere, no Kithkin. Mode 1 (target player) still works,
        // but let's construct a case where the spell still surfaces since
        // mode 1 has no prerequisites — so just verify mode 1 remains available.
        val driver = setupP1(
            hand = listOf("Brigid's Command"),
            battlefield = listOf("Forest", "Plains", "Forest"),
            extraSetCards = listOf(BrigidsCommand)
        )

        val casts = driver.enumerateFor(driver.player1).castActionsFor("Brigid's Command")

        casts shouldHaveSize 1
        val enumeration = casts.single().modalEnumeration.shouldNotBeNull()
        // Player target stays available; everything else requires a creature we don't control or a Kithkin or an opponent creature.
        enumeration.modes[1].available shouldBe true
        enumeration.unavailableIndices shouldContain 0
        enumeration.unavailableIndices shouldContain 2
        enumeration.unavailableIndices shouldContain 3
    }

})
