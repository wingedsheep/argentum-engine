package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Shimmerwilds Growth.
 *
 * Shimmerwilds Growth: {1}{G}
 * Enchantment — Aura
 *   Enchant land
 *   As this Aura enters, choose a color.
 *   Enchanted land is the chosen color.
 *   Whenever enchanted land is tapped for mana, its controller adds an additional
 *   one mana of the chosen color.
 */
class ShimmerwildsGrowthTest : FunSpec({

    // Basic land with an explicit mana ability so we can reference its ID in tests.
    val TestForest = basicLand("Forest") {}
    val manaAbilityId = TestForest.activatedAbilities[0].id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        // Register our TestForest first, then TestCards.all (which includes basic lands
        // from real sets). The registry keeps the last-registered card per name.
        driver.registerCards(TestCards.all + listOf(TestForest))
        return driver
    }

    test("Enchanted land becomes the chosen color at layer 5") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        val growth = driver.putCardInHand(activePlayer, "Shimmerwilds Growth")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, growth, listOf(forest))
        driver.bothPass()

        // Aura is resolving — the EntersWithChoice replacement pauses for color choice.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.BLUE))

        // Chosen color recorded on the aura.
        val auraId = driver.state.getBattlefield().first { entityId ->
            val name = driver.state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
            name == "Shimmerwilds Growth"
        }
        val chosen = driver.state.getEntity(auraId)!!.get<ChosenColorComponent>()
        chosen.shouldNotBeNull()
        chosen.color shouldBe Color.BLUE

        // Aura is attached to the Forest.
        driver.state.getEntity(auraId)!!.get<AttachedToComponent>()!!.targetId shouldBe forest

        // Forest is now blue (in addition to its types).
        val projected = projector.project(driver.state)
        projected.hasColor(forest, Color.BLUE) shouldBe true
        // Still a Forest subtype.
        projected.hasSubtype(forest, "Forest") shouldBe true
    }

    test("Tapping enchanted land produces two mana of the chosen color") {
        // "Enchanted land is the chosen color" means the land's own {T}: Add {color}
        // ability produces the chosen color instead of its normal output. The additional
        // mana trigger stacks on top: a Forest enchanted with chosen Blue taps for {U}{U}.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        val growth = driver.putCardInHand(activePlayer, "Shimmerwilds Growth")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, growth, listOf(forest))
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.BLUE))

        // Cost was {1}{G}; giveMana deposited 2G, spell consumed both. Pool should be empty.
        val poolBefore = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        poolBefore.green shouldBe 0
        poolBefore.blue shouldBe 0

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = forest,
                abilityId = manaAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Forest's base {G} is swapped to {U} + 1 extra {U} from the bonus trigger.
        val pool = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        pool.green shouldBe 0
        pool.blue shouldBe 2
    }

    test("Enchanted Mountain with Blue chosen taps for two blue (not 1R + 1U)") {
        // Regression: originally the Mountain's intrinsic {R} was not overridden,
        // so tapping gave {R}{U}. Shimmerwilds Growth should remap the land's own
        // mana entirely to the chosen color.
        val TestMountain = basicLand("Mountain") {}
        val mountainManaAbilityId = TestMountain.activatedAbilities[0].id

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestMountain))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mountain = driver.putPermanentOnBattlefield(activePlayer, "Mountain")
        val growth = driver.putCardInHand(activePlayer, "Shimmerwilds Growth")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, growth, listOf(mountain))
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.BLUE))

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mountain,
                abilityId = mountainManaAbilityId
            )
        ).isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        pool.red shouldBe 0
        pool.blue shouldBe 2
    }

    test("Tap-for-mana UI description reflects the chosen color") {
        // Regression: button used to say "Add {R}" even on an enchanted Mountain with
        // Blue chosen. Should now say "Add {U}".
        val TestMountain = basicLand("Mountain") {}

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestMountain))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mountain = driver.putPermanentOnBattlefield(activePlayer, "Mountain")
        val growth = driver.putCardInHand(activePlayer, "Shimmerwilds Growth")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, growth, listOf(mountain))
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.BLUE))

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val legalActions = enumerator.enumerate(driver.state, activePlayer, EnumerationMode.FULL)
        val mountainManaAction = legalActions.firstOrNull {
            it.isManaAbility && (it.action as? ActivateAbility)?.sourceId == mountain
        }
        mountainManaAction.shouldNotBeNull()
        mountainManaAction.description shouldBe "{T}: Add {U}"
    }

    test("Choosing green stacks with the land's own green production") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        val growth = driver.putCardInHand(activePlayer, "Shimmerwilds Growth")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, growth, listOf(forest))
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.GREEN))

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = forest,
                abilityId = manaAbilityId
            )
        ).isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        pool.green shouldBe 2
    }
})
