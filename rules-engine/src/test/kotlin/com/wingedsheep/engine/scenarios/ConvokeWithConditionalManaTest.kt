package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Convoke (CR 702.51) paid alongside conditional ("spend this mana only to …") floating mana.
 *
 * Driven by Ashling, Rimebound: "add two mana of any one color. Spend this mana only to cast
 * spells with mana value 4 or greater." A convoked spell keeps its printed mana value
 * (CR 202.3 — convoke pays part of the cost, it doesn't reduce it), so the restricted mana
 * stays eligible no matter how many pips convoke covers.
 */
class ConvokeWithConditionalManaTest : FunSpec({

    // MV 5 — eligible for Ashling's restricted mana.
    val convokeBehemoth = card("Convoke Behemoth") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Elemental Beast"
        power = 5
        toughness = 5
        oracleText = "Convoke"
        keywords(Keyword.CONVOKE)
    }

    // MV 6 — needs one more mana than the restricted pool + convoke can supply on its own.
    val convokeColossus = card("Convoke Colossus") {
        manaCost = "{4}{R}{R}"
        typeLine = "Creature — Elemental Giant"
        power = 6
        toughness = 6
        oracleText = "Convoke"
        keywords(Keyword.CONVOKE)
    }

    // MV 3 — never eligible for Ashling's restricted mana, convoke or not.
    val convokeSprite = card("Convoke Sprite") {
        manaCost = "{1}{R}{R}"
        typeLine = "Creature — Faerie Scout"
        power = 2
        toughness = 2
        oracleText = "Convoke"
        keywords(Keyword.CONVOKE)
    }

    val redSoldier = card("Red Soldier") {
        manaCost = "{R}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(convokeBehemoth, convokeColossus, convokeSprite, redSoldier)
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), skipMulligans = true)
        return driver
    }

    /** Two untapped red creatures the caster can tap for convoke. */
    fun twoRedCreatures(driver: GameTestDriver, player: EntityId): Pair<EntityId, EntityId> {
        val a = driver.putCreatureOnBattlefield(player, "Red Soldier")
        val b = driver.putCreatureOnBattlefield(player, "Red Soldier")
        return a to b
    }

    fun castActionFor(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean {
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        return enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            .any { (it.action as? CastSpell)?.cardId == cardId }
    }

    // ---------------------------------------------------------------- payment paths

    test("FromPool: convoke covers {R}{R}, restricted MV4+ mana pays the generic {3}") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (c1, c2) = twoRedCreatures(driver, player)
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Behemoth")
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        c1 to ConvokePayment(color = Color.RED),
                        c2 to ConvokePayment(color = Color.RED)
                    )
                )
            )
        )

        result.isSuccess shouldBe true
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("AutoPay: convoke covers {R}{R}, restricted MV4+ mana pays the generic {3}") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (c1, c2) = twoRedCreatures(driver, player)
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Behemoth")
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.AutoPay,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        c1 to ConvokePayment(color = Color.RED),
                        c2 to ConvokePayment(color = Color.RED)
                    )
                )
            )
        )

        result.isSuccess shouldBe true
    }

    test("convoke-reduced payment keeps the printed mana value: restricted mana stays eligible") {
        // Cost {3}{R}{R} convoked down to {1} of real mana — the spell is still MV 5, so the
        // MV4+ restricted mana is legal to spend on it.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val creatures = (1..4).map { driver.putCreatureOnBattlefield(player, "Red Soldier") }
        driver.giveRestrictedMana(player, Color.RED, 1, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Behemoth")
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        creatures[0] to ConvokePayment(color = Color.RED),
                        creatures[1] to ConvokePayment(color = Color.RED),
                        creatures[2] to ConvokePayment(color = null),
                        creatures[3] to ConvokePayment(color = null)
                    )
                )
            )
        )

        result.isSuccess shouldBe true
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    // ---------------------------------------------------------------- legal actions (UI)

    test("enumerator offers the convoke spell when only convoke + restricted mana can pay") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        twoRedCreatures(driver, player)
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Behemoth")
        castActionFor(driver, player, spellId) shouldBe true
    }

    test("enumerator does not offer an MV3 convoke spell payable only with MV4+ restricted mana") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // One creature for a single {R} pip; the rest of {1}{R} would need the restricted mana.
        driver.putCreatureOnBattlefield(player, "Red Soldier")
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Sprite")
        castActionFor(driver, player, spellId) shouldBe false
    }

    test("MV3 convoke spell cannot be paid with MV4+ restricted mana") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (c1, c2) = twoRedCreatures(driver, player)
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Sprite")
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        c1 to ConvokePayment(color = Color.RED),
                        c2 to ConvokePayment(color = Color.RED)
                    )
                )
            )
        )

        result.isSuccess shouldBe false
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 3
    }

    // ------------------------------------------------- combined with other mana abilities

    test("restricted mana + an any-color mana creature + convoke pay a {4}{R}{R} spell") {
        // Birds of Paradise stands in for Great Forest Druid ("{T}: Add one mana of any color").
        // {4}{R}{R}: convoke two red creatures for {R}{R}, 3 restricted mana + the Bird's mana
        // cover the remaining {4}.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (c1, c2) = twoRedCreatures(driver, player)
        val bird = driver.putCreatureOnBattlefield(player, "Birds of Paradise")
        driver.removeSummoningSickness(bird)
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Colossus")
        castActionFor(driver, player, spellId) shouldBe true

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.AutoPay,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        c1 to ConvokePayment(color = Color.RED),
                        c2 to ConvokePayment(color = Color.RED)
                    )
                )
            )
        )
        result.isSuccess shouldBe true
    }

    test("restricted mana pays the colored pips while convoke pays the generic") {
        // {3}{R}{R} with three *white* creatures: convoke can only cover the generic {3}, so the
        // two {R} pips must come from Ashling's restricted red mana.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val whiteCreatures = (1..3).map { driver.putCreatureOnBattlefield(player, "Savannah Lions") }
        driver.giveRestrictedMana(player, Color.RED, 2, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Behemoth")
        castActionFor(driver, player, spellId) shouldBe true

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = whiteCreatures.associateWith { ConvokePayment(color = null) }
                )
            )
        )
        result.isSuccess shouldBe true
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("ability convoke + ability-only restricted mana (Heirloom Epic)") {
        // "{4}, {T}: Draw a card. For each mana in this ability's activation cost, you may tap an
        // untapped creature you control rather than pay that mana." Two creatures cover {2}; the
        // other {2} comes from mana restricted to ability activations.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val epic = driver.putPermanentOnBattlefield(player, "Heirloom Epic")
        val (c1, c2) = twoRedCreatures(driver, player)
        driver.giveRestrictedMana(player, Color.RED, 2, ManaRestriction.AbilityActivationOnly)

        val abilityId = driver.cardRegistry.requireCard("Heirloom Epic").activatedAbilities.first().id
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val offered = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            .any { (it.action as? ActivateAbility)?.sourceId == epic }
        offered shouldBe true

        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = epic,
                abilityId = abilityId,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        c1 to ConvokePayment(color = null),
                        c2 to ConvokePayment(color = null)
                    )
                )
            )
        )
        result.isSuccess shouldBe true
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("a mana creature tapped for convoke can't also make mana") {
        // Only resource besides the pool is the Bird: convoking it pays one pip but then its
        // mana ability is gone. {3}{R}{R} needs 5; convoke(1) + 3 restricted = 4 → not castable.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bird = driver.putCreatureOnBattlefield(player, "Birds of Paradise")
        driver.removeSummoningSickness(bird)
        driver.giveRestrictedMana(player, Color.RED, 3, ManaRestriction.SpellsWithManaValueAtLeast(4))

        val spellId = driver.putCardInHand(player, "Convoke Behemoth")
        castActionFor(driver, player, spellId) shouldBe false

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.AutoPay,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(bird to ConvokePayment(color = Color.RED))
                )
            )
        )
        result.isSuccess shouldBe false
    }
})
