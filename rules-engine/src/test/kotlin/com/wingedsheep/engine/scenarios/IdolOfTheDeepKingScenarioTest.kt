package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.IdolOfTheDeepKing
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Idol of the Deep King // Sovereign's Macuahuitl (LCI #155) — {2}{R} Artifact // Artifact — Equipment.
 *
 * Front: Flash; "When this artifact enters, it deals 2 damage to any target.";
 *        Craft with artifact {2}{R} (exactly one artifact material, CR 702.167).
 * Back:  "When this Equipment enters, attach it to target creature you control.";
 *        Equipped creature gets +2/+0; Equip {2}.
 *
 * Covers:
 *  - Front ETB: cast from hand, targeted trigger deals 2 damage (kills a 2/1).
 *  - Flash: the front face can be cast at instant speed (upkeep, not a main phase).
 *  - Craft end-to-end: pays {2}{R}, exiles self + exactly one artifact material, returns
 *    transformed as Sovereign's Macuahuitl; the back-face ETB attaches it to a target
 *    creature you control, which gets +2/+0 via projection.
 *  - Equip {2}: moves the Equipment to another creature at sorcery speed; buff follows.
 *  - Negative: craft rejected when the supplied material is a creature, not an artifact.
 */
class IdolOfTheDeepKingScenarioTest : FunSpec({

    // Plain artifact — a legal craft material.
    val trinket = card("Test Idol Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    val projector = StateProjector()

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(trinket))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        return driver
    }

    // Front face's only activated ability is the Craft.
    fun craftAbilityId() = IdolOfTheDeepKing.activatedAbilities.single().id

    // Back face's only activated ability is Equip {2}.
    fun equipAbilityId() = IdolOfTheDeepKing.backFace!!.activatedAbilities.single().id

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && guard++ < 20) bothPass()
        pendingDecision.shouldNotBeNull()
    }

    /**
     * Shared craft setup: Idol + a trinket material + a Centaur Courser (3/3) on p1's
     * battlefield; crafts in the precombat main and answers the back-face ETB attach
     * trigger by attaching to the Courser. Returns (idol, courser).
     */
    fun GameTestDriver.craftOntoCourser(p1: EntityId): Pair<EntityId, EntityId> {
        val idol = putPermanentOnBattlefield(p1, "Idol of the Deep King")
        val material = putPermanentOnBattlefield(p1, "Test Idol Trinket")
        val courser = putCreatureOnBattlefield(p1, "Centaur Courser") // 3/3
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(p1, Color.RED, 3)

        submit(
            ActivateAbility(
                playerId = p1,
                sourceId = idol,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(material))
            )
        ).isSuccess shouldBe true

        // Resolve the craft ability; the back face enters and its ETB attach trigger
        // asks for a target creature you control.
        resolveUntilDecision()
        submitTargetSelection(p1, listOf(courser)).error shouldBe null
        drainStack()

        // Material exiled; source back on the battlefield.
        state.getZone(ZoneKey(p1, Zone.EXILE)).shouldContain(material)
        return idol to courser
    }

    test("front ETB: cast from hand, trigger deals 2 damage to target opponent creature") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val guide = driver.putCreatureOnBattlefield(opponent, "Goblin Guide") // 2/1
        val idol = driver.putCardInHand(p1, "Idol of the Deep King")
        driver.giveMana(p1, Color.RED, 3)
        driver.castSpell(p1, idol, emptyList())

        // Resolve the artifact; its ETB trigger asks for "any target".
        driver.resolveUntilDecision()
        driver.submitTargetSelection(p1, listOf(guide)).error shouldBe null
        driver.drainStack()

        // 2 damage kills the 2/1; the Idol is on the battlefield as its front face.
        driver.findPermanent(opponent, "Goblin Guide") shouldBe null
        val idolCard = driver.state.getEntity(idol)!!.get<CardComponent>()!!
        idolCard.name shouldBe "Idol of the Deep King"
    }

    test("flash: can be cast at instant speed during upkeep") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)

        val guide = driver.putCreatureOnBattlefield(opponent, "Goblin Guide") // 2/1
        val idol = driver.putCardInHand(p1, "Idol of the Deep King")
        driver.passPriorityUntil(Step.UPKEEP) // not a main phase — instant speed only
        driver.giveMana(p1, Color.RED, 3)

        driver.castSpell(p1, idol, emptyList()).error shouldBe null

        driver.resolveUntilDecision()
        driver.submitTargetSelection(p1, listOf(guide)).error shouldBe null
        driver.drainStack()

        driver.findPermanent(opponent, "Goblin Guide") shouldBe null
        driver.findPermanent(p1, "Idol of the Deep King") shouldNotBe null
    }

    test("craft with artifact: exiles self + one artifact, returns as Sovereign's Macuahuitl and ETB-attaches for +2/+0") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val (idol, courser) = driver.craftOntoCourser(p1)

        // Back face on the battlefield: an Artifact — Equipment named Sovereign's Macuahuitl.
        val container = driver.state.getEntity(idol)
        container.shouldNotBeNull()
        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Sovereign's Macuahuitl"
        card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT)
        card.typeLine.subtypes.shouldContain(Subtype.EQUIPMENT)

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // ETB trigger attached it to the chosen creature: +2/+0.
        container.get<AttachedToComponent>()?.targetId shouldBe courser
        projector.getProjectedPower(driver.state, courser) shouldBe 5
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("equip {2} moves the Equipment to another creature at sorcery speed") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val (idol, courser) = driver.craftOntoCourser(p1)
        val guide = driver.putCreatureOnBattlefield(p1, "Goblin Guide") // 2/1

        driver.giveColorlessMana(p1, 2)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = idol,
                abilityId = equipAbilityId(),
                targets = listOf(ChosenTarget.Permanent(guide))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Re-attached to the new creature; the buff moves with it.
        driver.state.getEntity(idol)?.get<AttachedToComponent>()?.targetId shouldBe guide
        projector.getProjectedPower(driver.state, guide) shouldBe 4
        projector.getProjectedToughness(driver.state, guide) shouldBe 1
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("negative: craft rejected when the supplied material is a creature, not an artifact") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val idol = driver.putPermanentOnBattlefield(p1, "Idol of the Deep King")
        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser") // not an artifact
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = idol,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(courser))
            )
        )
        result.isSuccess shouldBe false

        // Nothing moved: Idol still the front face on the battlefield, Courser untouched.
        driver.state.getEntity(idol)!!.get<CardComponent>()!!.name shouldBe "Idol of the Deep King"
        driver.findPermanent(p1, "Centaur Courser") shouldNotBe null
    }
})
