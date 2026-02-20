package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Contested Cliffs.
 *
 * Contested Cliffs
 * Land
 * {T}: Add {C}.
 * {R}{G}, {T}: Target Beast creature you control fights target creature an opponent controls.
 */
class ContestedCliffsTest : FunSpec({

    val ContestedCliffs = card("Contested Cliffs") {
        typeLine = "Land"

        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{R}{G}"),
                Costs.Tap
            )
            val beast = target("Beast creature you control", TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Beast").youControl())
            ))
            val opponentCreature = target("creature an opponent controls", TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
            ))
            effect = Effects.Fight(beast, opponentCreature)
        }
    }

    val fightAbilityId = ContestedCliffs.activatedAbilities[1].id

    val TestBeast = CardDefinition.creature(
        name = "Test Beast",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 4,
        toughness = 4
    )

    val SmallCreature = CardDefinition.creature(
        name = "Small Creature",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Human")),
        power = 2,
        toughness = 2
    )

    val BigCreature = CardDefinition.creature(
        name = "Big Creature",
        manaCost = ManaCost.parse("{4}"),
        subtypes = setOf(Subtype("Human")),
        power = 5,
        toughness = 5
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ContestedCliffs, TestBeast, SmallCreature, BigCreature))
        return driver
    }

    test("fight ability - Beast deals damage to opponent's creature and vice versa") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cliffs = driver.putPermanentOnBattlefield(activePlayer, "Contested Cliffs")
        val beast = driver.putCreatureOnBattlefield(activePlayer, "Test Beast") // 4/4
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Small Creature") // 2/2

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = cliffs,
                abilityId = fightAbilityId,
                targets = listOf(
                    ChosenTarget.Permanent(beast),
                    ChosenTarget.Permanent(opponentCreature)
                )
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Beast (4/4) takes 2 damage from Small Creature
        val beastDamage = driver.state.getEntity(beast)?.get<DamageComponent>()?.amount ?: 0
        beastDamage shouldBe 2

        // Small Creature (2/2) takes 4 damage from Beast - should be dead (lethal damage)
        driver.findPermanent(opponent, "Small Creature") shouldBe null
    }

    test("fight ability - both creatures survive when neither deals lethal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cliffs = driver.putPermanentOnBattlefield(activePlayer, "Contested Cliffs")
        val beast = driver.putCreatureOnBattlefield(activePlayer, "Test Beast") // 4/4
        val bigCreature = driver.putCreatureOnBattlefield(opponent, "Big Creature") // 5/5

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = cliffs,
                abilityId = fightAbilityId,
                targets = listOf(
                    ChosenTarget.Permanent(beast),
                    ChosenTarget.Permanent(bigCreature)
                )
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Beast (4/4) takes 5 damage from Big Creature - should be dead (lethal damage)
        driver.findPermanent(activePlayer, "Test Beast") shouldBe null

        // Big Creature (5/5) takes 4 damage from Beast - survives
        val bigCreatureDamage = driver.state.getEntity(bigCreature)?.get<DamageComponent>()?.amount ?: 0
        bigCreatureDamage shouldBe 4
    }

    test("fight ability - cannot target non-Beast creature you control") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cliffs = driver.putPermanentOnBattlefield(activePlayer, "Contested Cliffs")
        val nonBeast = driver.putCreatureOnBattlefield(activePlayer, "Small Creature") // 2/2 Human, not a Beast
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Small Creature")

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = cliffs,
                abilityId = fightAbilityId,
                targets = listOf(
                    ChosenTarget.Permanent(nonBeast),
                    ChosenTarget.Permanent(opponentCreature)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("fight ability - cannot target your own creature as the opponent's creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cliffs = driver.putPermanentOnBattlefield(activePlayer, "Contested Cliffs")
        val beast = driver.putCreatureOnBattlefield(activePlayer, "Test Beast")
        val ownCreature = driver.putCreatureOnBattlefield(activePlayer, "Small Creature")

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = cliffs,
                abilityId = fightAbilityId,
                targets = listOf(
                    ChosenTarget.Permanent(beast),
                    ChosenTarget.Permanent(ownCreature)
                )
            )
        )
        result.isSuccess shouldBe false
    }
})
