package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Tests for Unholy Grotto.
 *
 * Unholy Grotto
 * Land
 * {T}: Add {C}.
 * {B}, {T}: Put target Zombie card from your graveyard on top of your library.
 */
class UnholyGrottoTest : FunSpec({

    val UnholyGrotto = card("Unholy Grotto") {
        typeLine = "Land"

        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }

        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
            target = TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Any.withSubtype("Zombie").ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
            effect = MoveToZoneEffect(
                target = EffectTarget.ContextTarget(0),
                destination = Zone.LIBRARY,
                placement = ZonePlacement.Top
            )
        }
    }

    val abilityId = UnholyGrotto.activatedAbilities[1].id

    val FesteringGoblin = CardDefinition.creature(
        name = "Festering Goblin",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    val GluttonousZombie = CardDefinition.creature(
        name = "Gluttonous Zombie",
        manaCost = ManaCost.parse("{4}{B}"),
        subtypes = setOf(Subtype("Zombie")),
        power = 3,
        toughness = 3
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UnholyGrotto, FesteringGoblin, GluttonousZombie))
        return driver
    }

    test("put target Zombie from graveyard on top of library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grotto = driver.putPermanentOnBattlefield(activePlayer, "Unholy Grotto")
        val zombie = driver.putCardInGraveyard(activePlayer, "Festering Goblin")

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = grotto,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(zombie, activePlayer, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Zombie should no longer be in the graveyard
        driver.getGraveyardCardNames(activePlayer) shouldNotContain "Festering Goblin"

        // Zombie should be on top of the library
        val library = driver.state.getLibrary(activePlayer)
        library.size shouldBeGreaterThan 0
        library.first() shouldBe zombie
    }

    test("cannot target non-Zombie card in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grotto = driver.putPermanentOnBattlefield(activePlayer, "Unholy Grotto")
        val bears = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = grotto,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(bears, activePlayer, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot target Zombie in opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grotto = driver.putPermanentOnBattlefield(activePlayer, "Unholy Grotto")
        val opponentZombie = driver.putCardInGraveyard(opponent, "Festering Goblin")

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = grotto,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(opponentZombie, opponent, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate without paying black mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grotto = driver.putPermanentOnBattlefield(activePlayer, "Unholy Grotto")
        val zombie = driver.putCardInGraveyard(activePlayer, "Festering Goblin")

        // No mana given
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = grotto,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(zombie, activePlayer, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe false
    }

    test("can target different Zombies in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grotto = driver.putPermanentOnBattlefield(activePlayer, "Unholy Grotto")
        driver.putCardInGraveyard(activePlayer, "Festering Goblin")
        val zombie2 = driver.putCardInGraveyard(activePlayer, "Gluttonous Zombie")

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = grotto,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(zombie2, activePlayer, Zone.GRAVEYARD))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Gluttonous Zombie should be on top of library
        val library = driver.state.getLibrary(activePlayer)
        library.first() shouldBe zombie2

        // Festering Goblin should still be in the graveyard
        driver.getGraveyardCardNames(activePlayer) shouldContain "Festering Goblin"
    }
})
