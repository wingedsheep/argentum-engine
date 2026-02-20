package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Silvos, Rogue Elemental.
 *
 * Silvos, Rogue Elemental: {3}{G}{G}{G}
 * Legendary Creature — Elemental
 * 8/5
 * Trample
 * {G}: Regenerate Silvos.
 */
class SilvosRogueElementalTest : FunSpec({

    val SilvosRogueElemental = card("Silvos, Rogue Elemental") {
        manaCost = "{3}{G}{G}{G}"
        typeLine = "Legendary Creature — Elemental"
        power = 8
        toughness = 5

        keywords(Keyword.TRAMPLE)

        activatedAbility {
            cost = Costs.Mana("{G}")
            effect = RegenerateEffect(EffectTarget.Self)
        }
    }

    // A test spell that deals 5 damage to target creature
    val TestBlast = CardDefinition.instant(
        name = "Test Blast",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Test Blast deals 5 damage to target creature.",
        script = CardScript.spell(
            effect = DealDamageEffect(5, EffectTarget.ContextTarget(0)),
            targets = arrayOf(TargetObject(filter = TargetFilter(GameObjectFilter.Creature)))
        )
    )

    val regenAbilityId = SilvosRogueElemental.activatedAbilities.first().id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilvosRogueElemental, TestBlast))
        return driver
    }

    test("Silvos has correct stats") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val silvos = driver.putCreatureOnBattlefield(player, "Silvos, Rogue Elemental")

        projector.getProjectedPower(driver.state, silvos) shouldBe 8
        projector.getProjectedToughness(driver.state, silvos) shouldBe 5
    }

    test("Silvos has trample") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val silvos = driver.putCreatureOnBattlefield(player, "Silvos, Rogue Elemental")

        projector.getProjectedKeywords(driver.state, silvos) shouldBe setOf(Keyword.TRAMPLE)
    }

    test("Silvos regenerates when it would be destroyed by lethal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val silvos = driver.putCreatureOnBattlefield(player, "Silvos, Rogue Elemental")
        driver.removeSummoningSickness(silvos)

        // Give mana for regeneration and the blast
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.RED, 1)

        // Activate regeneration ability
        val regenResult = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = silvos,
                abilityId = regenAbilityId,
                targets = emptyList()
            )
        )
        regenResult.isSuccess shouldBe true

        // Resolve the regeneration ability
        driver.bothPass()

        // Cast Test Blast dealing 5 damage to Silvos
        val blast = driver.putCardInHand(player, "Test Blast")
        val castResult = driver.castSpell(player, blast, listOf(silvos))
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // After SBAs, Silvos should still be on the battlefield (regenerated)
        driver.findPermanent(player, "Silvos, Rogue Elemental") shouldNotBe null

        // Silvos should be tapped after regeneration
        driver.isTapped(silvos) shouldBe true

        // It should NOT be in the graveyard
        driver.getGraveyardCardNames(player) shouldNotContain "Silvos, Rogue Elemental"
    }

    test("Silvos dies without regeneration shield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val silvos = driver.putCreatureOnBattlefield(player, "Silvos, Rogue Elemental")

        // Cast Test Blast dealing 5 damage to Silvos without regeneration
        val blast = driver.putCardInHand(player, "Test Blast")
        driver.giveMana(player, Color.RED, 1)
        val castResult = driver.castSpell(player, blast, listOf(silvos))
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Silvos should be destroyed
        driver.findPermanent(player, "Silvos, Rogue Elemental") shouldBe null
        driver.getGraveyardCardNames(player) shouldContain "Silvos, Rogue Elemental"
    }

    test("regeneration ability costs one green mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val silvos = driver.putCreatureOnBattlefield(player, "Silvos, Rogue Elemental")
        driver.removeSummoningSickness(silvos)

        // Without mana, the ability should fail
        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = silvos,
                abilityId = regenAbilityId,
                targets = emptyList()
            )
        )
        result.isSuccess shouldBe false
    }
})
