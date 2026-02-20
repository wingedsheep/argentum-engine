package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

/**
 * Tests for Dwarven Blastminer.
 *
 * Dwarven Blastminer: {1}{R}
 * Creature — Dwarf
 * 1/1
 * {2}{R}, {T}: Destroy target nonbasic land.
 * Morph {R}
 */
class DwarvenBlastminerTest : FunSpec({

    val blastminerAbilityId = AbilityId(UUID.randomUUID().toString())

    val DwarvenBlastminer = CardDefinition(
        name = "Dwarven Blastminer",
        manaCost = ManaCost.parse("{1}{R}"),
        typeLine = TypeLine.creature(setOf(Subtype("Dwarf"))),
        oracleText = "{2}{R}, {T}: Destroy target nonbasic land.\nMorph {R}",
        creatureStats = CreatureStats(1, 1),
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{R}"))),
        script = CardScript.permanent(
            ActivatedAbility(
                id = blastminerAbilityId,
                cost = AbilityCost.Composite(
                    listOf(AbilityCost.Mana(ManaCost.parse("{2}{R}")), AbilityCost.Tap)
                ),
                effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true),
                targetRequirement = TargetPermanent(
                    filter = TargetFilter(
                        GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.IsLand,
                                CardPredicate.Not(CardPredicate.IsBasicLand)
                            )
                        )
                    )
                )
            )
        )
    )

    val TestNonbasicLand = CardDefinition(
        name = "Test Nonbasic Land",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND)
        ),
        oracleText = "{T}: Add {C}."
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DwarvenBlastminer, TestNonbasicLand))
        return driver
    }

    test("destroys target nonbasic land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Blastminer on battlefield and remove summoning sickness
        val blastminer = driver.putCreatureOnBattlefield(activePlayer, "Dwarven Blastminer")
        driver.removeSummoningSickness(blastminer)

        // Put a nonbasic land on opponent's battlefield
        val nonbasicLand = driver.putLandOnBattlefield(opponent, "Test Nonbasic Land")
        driver.findPermanent(opponent, "Test Nonbasic Land") shouldNotBe null

        // Give mana to activate ability
        driver.giveMana(activePlayer, Color.RED, 3)

        // Activate the ability targeting the nonbasic land
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = blastminer,
                abilityId = blastminerAbilityId,
                targets = listOf(ChosenTarget.Permanent(nonbasicLand))
            )
        )
        result.isSuccess shouldBe true

        // Blastminer should be tapped
        driver.isTapped(blastminer) shouldBe true

        // Both pass to resolve the ability
        driver.bothPass()

        // The nonbasic land should be destroyed
        driver.findPermanent(opponent, "Test Nonbasic Land") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Test Nonbasic Land"
    }

    test("cannot target basic lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Blastminer on battlefield and remove summoning sickness
        val blastminer = driver.putCreatureOnBattlefield(activePlayer, "Dwarven Blastminer")
        driver.removeSummoningSickness(blastminer)

        // Put a basic land on opponent's battlefield
        val basicLand = driver.putLandOnBattlefield(opponent, "Forest")

        // Give mana to activate ability
        driver.giveMana(activePlayer, Color.RED, 3)

        // Attempt to activate the ability targeting a basic land - should fail
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = blastminer,
                abilityId = blastminerAbilityId,
                targets = listOf(ChosenTarget.Permanent(basicLand))
            )
        )
        result.isSuccess shouldBe false

        // The basic land should still be there
        driver.findPermanent(opponent, "Forest") shouldNotBe null
    }

    test("Blastminer taps as part of cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val blastminer = driver.putCreatureOnBattlefield(activePlayer, "Dwarven Blastminer")
        driver.removeSummoningSickness(blastminer)

        val nonbasicLand = driver.putLandOnBattlefield(opponent, "Test Nonbasic Land")

        driver.giveMana(activePlayer, Color.RED, 3)

        // Blastminer should be untapped initially
        driver.isTapped(blastminer) shouldBe false

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = blastminer,
                abilityId = blastminerAbilityId,
                targets = listOf(ChosenTarget.Permanent(nonbasicLand))
            )
        )

        // Blastminer should now be tapped
        driver.isTapped(blastminer) shouldBe true
    }
})
