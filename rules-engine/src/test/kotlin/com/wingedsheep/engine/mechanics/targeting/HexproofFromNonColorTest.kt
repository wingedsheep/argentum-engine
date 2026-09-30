package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.HexproofFromComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Hexproof from non<color>" — `ProtectionScope.NonColor` (CR 702.11d over the quality "nongreen").
 *
 * The rules pinned here:
 * - an opponent's spell or ability source that isn't the color can't target it — colored or
 *   **colorless** (CR 105.2c: a colorless object has no color, so it is nongreen);
 * - a source that *is* the color can, even when it is also another color (a green-white spell is
 *   green);
 * - it only stops opponents: the permanent's controller targets it freely (CR 702.11d);
 * - the check holds at every targeting site: legal-target enumeration, cast/activation validation,
 *   and the resolution-time re-check (a target that gained it after being targeted is illegal).
 */
class HexproofFromNonColorTest : FunSpec({

    val colorlessPinger = card("Test Colorless Pinger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val greenPinger = card("Test Green Pinger") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val selesnyaBolt = card("Test Selesnya Bolt") {
        manaCost = "{G}{W}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(3, t)
        }
    }
    val nongreenHexproof = card("Test Nongreen Warden") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Troll"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.hexproofFromNon(Color.GREEN))
    }

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(colorlessPinger, greenPinger, selesnyaBolt, nongreenHexproof))
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun damageOn(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun abilityId(def: com.wingedsheep.sdk.model.CardDefinition) = def.activatedAbilities.single().id

    test("the printed keyword projects HEXPROOF_FROM_NON_GREEN") {
        val driver = newDriver()
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        driver.state.projectedState.hasKeyword(warden, "HEXPROOF_FROM_NON_GREEN") shouldBe true
    }

    test("an opponent's nongreen spell can't target it") {
        val driver = newDriver()
        val caster = driver.player1
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)

        driver.castSpell(caster, bolt, listOf(warden)).outcome shouldNotBe Outcome.Done
    }

    test("an opponent's green spell can target it") {
        val driver = newDriver()
        val caster = driver.player1
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        val growth = driver.putCardInHand(caster, "Giant Growth")
        driver.giveMana(caster, Color.GREEN, 1)

        driver.castSpell(caster, growth, listOf(warden)).outcome shouldBe Outcome.Done
    }

    test("a green-white spell is green, so it can target it") {
        val driver = newDriver()
        val caster = driver.player1
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        val spell = driver.putCardInHand(caster, "Test Selesnya Bolt")
        driver.giveMana(caster, Color.GREEN, 1)
        driver.giveMana(caster, Color.WHITE, 1)

        driver.castSpell(caster, spell, listOf(warden)).outcome shouldBe Outcome.Done
        driver.bothPass()
        damageOn(driver, warden) shouldBe 3
    }

    test("an ability from an opponent's colorless source can't target it") {
        val driver = newDriver()
        val caster = driver.player1
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        val pinger = driver.putPermanentOnBattlefield(caster, "Test Colorless Pinger")

        driver.submit(
            ActivateAbility(
                playerId = caster,
                sourceId = pinger,
                abilityId = abilityId(colorlessPinger),
                targets = listOf(ChosenTarget.Permanent(warden))
            )
        ).outcome shouldNotBe Outcome.Done
    }

    test("an ability from an opponent's green source can target it") {
        val driver = newDriver()
        val caster = driver.player1
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        val pinger = driver.putCreatureOnBattlefield(caster, "Test Green Pinger")
        driver.removeSummoningSickness(pinger)

        driver.submit(
            ActivateAbility(
                playerId = caster,
                sourceId = pinger,
                abilityId = abilityId(greenPinger),
                targets = listOf(ChosenTarget.Permanent(warden))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        damageOn(driver, warden) shouldBe 1
    }

    test("its controller can target it with a nongreen spell") {
        val driver = newDriver()
        val me = driver.player1
        val warden = driver.putCreatureOnBattlefield(me, "Test Nongreen Warden")
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)

        driver.castSpell(me, bolt, listOf(warden)).outcome shouldBe Outcome.Done
    }

    test("legal-target enumeration leaves it out for nongreen sources and offers it to green ones") {
        val driver = newDriver()
        val caster = driver.player1
        val warden = driver.putCreatureOnBattlefield(driver.player2, "Test Nongreen Warden")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        val growth = driver.putCardInHand(caster, "Giant Growth")
        val pinger = driver.putPermanentOnBattlefield(caster, "Test Colorless Pinger")
        driver.giveMana(caster, Color.RED, 1)
        driver.giveMana(caster, Color.GREEN, 1)

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        fun castTargets(card: EntityId) =
            actions.single { (it.action as? CastSpell)?.cardId == card }.validTargets.orEmpty()

        castTargets(bolt) shouldNotContain warden
        castTargets(growth) shouldContain warden
        actions.single { (it.action as? ActivateAbility)?.sourceId == pinger }
            .validTargets.orEmpty() shouldNotContain warden
    }

    test("a target that gains it before resolution is illegal, and the spell fizzles") {
        val driver = newDriver()
        val caster = driver.player1
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)

        driver.castSpell(caster, bolt, listOf(bear)).outcome shouldBe Outcome.Done
        driver.addComponent(bear, HexproofFromComponent(nonColors = setOf(Color.GREEN)))
        driver.bothPass()

        damageOn(driver, bear) shouldBe 0
        driver.findPermanent(driver.player2, "Grizzly Bears") shouldBe bear
    }
})
