package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.CurseclothWrappings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cursecloth Wrappings (DFT #81) — {2}{B}{B} Artifact.
 *
 *   Zombies you control get +1/+1.
 *   {T}: Target creature card in your graveyard gains embalm until end of turn. The embalm cost is
 *   equal to its mana cost.
 *
 * Covers the whole Embalm (CR 702.128) path this card introduces: the runtime grant of a
 * *graveyard-activated* ability (as opposed to the cast-keyword grants harmonize/flashback use),
 * that ability being surfaced on a card sitting in the graveyard, and the three copy exceptions on
 * the token it makes — white instead of its other colors, Zombie in addition to its other types,
 * and no mana cost. The lord half then pumps the token it just made, which is the tidiest proof the
 * token really is a Zombie.
 */
class CurseclothWrappingsScenarioTest : FunSpec({

    // A green Bear to embalm. Green so "white instead of its other colors" is observable, and
    // {1}{G} so "the embalm cost is equal to its mana cost" is observable too.
    val greenBear = card("Wrappings Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // A non-creature card in the same graveyard — must never be a legal target.
    val instantCard = card("Wrappings Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Wrappings Test Bolt deals 3 damage to any target."
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CurseclothWrappings)
        driver.registerCard(greenBear)
        driver.registerCard(instantCard)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun wrappingsAbilityId(driver: GameTestDriver) =
        driver.cardRegistry.getCard("Cursecloth Wrappings")!!.activatedAbilities.first().id

    /** Put Cursecloth Wrappings out, drop [cardName] in the graveyard, and grant it embalm. */
    fun grantEmbalm(driver: GameTestDriver, cardName: String): Pair<EntityId, EntityId> {
        val me = driver.player1
        val wrappings = driver.putPermanentOnBattlefield(me, "Cursecloth Wrappings")
        val graveyardCard = driver.putCardInGraveyard(me, cardName)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = wrappings,
                abilityId = wrappingsAbilityId(driver),
                targets = listOf(ChosenTarget.Card(graveyardCard, me, Zone.GRAVEYARD)),
            )
        )
        repeat(4) { if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass() }
        return wrappings to graveyardCard
    }

    test("the granted embalm ability exiles the card and makes a white Zombie token copy with no mana cost") {
        val driver = newDriver()
        val me = driver.player1
        val (_, bear) = grantEmbalm(driver, "Wrappings Test Bear")

        val grant = driver.state.grantedActivatedAbilities.singleOrNull { it.entityId == bear }
            .shouldNotBeNull()
        val composite = grant.ability.cost.shouldBeInstanceOf<AbilityCost.Composite>()
        withClue("'The embalm cost is equal to its mana cost' — {1}{G}, plus exiling the card") {
            composite.costs.firstNotNullOfOrNull { it.manaCostOrNull } shouldBe ManaCost.parse("{1}{G}")
            composite.costs shouldContain AbilityCost.ExileSelf
        }

        // Embalm is sorcery-speed and costs {1}{G} on top of exiling the card. Pay it from mana of
        // the right colors so nothing about the cost itself is being finessed.
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(playerId = me, sourceId = bear, abilityId = grant.ability.id)
        )
        repeat(4) { if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass() }

        withClue("The card is exiled as part of the cost, not left in the graveyard") {
            driver.state.getZone(me, Zone.EXILE) shouldContain bear
            driver.getGraveyard(me) shouldNotContain bear
        }

        val token = driver.findPermanent(me, "Wrappings Test Bear").shouldNotBeNull()
        val tokenCard = driver.state.getEntity(token)?.get<CardComponent>().shouldNotBeNull()
        withClue("It is a token, not the card itself, and it copies the printed 2/2 Bear") {
            driver.state.getEntity(token)?.has<TokenComponent>()?.shouldBeTrue()
            tokenCard.baseStats?.basePower shouldBe 2
            tokenCard.typeLine.subtypes shouldContain Subtype("Bear")
        }
        withClue("CR 702.128a — white instead of its other colors, Zombie in addition to its types, no mana cost") {
            tokenCard.colors shouldBe setOf(Color.WHITE)
            tokenCard.typeLine.subtypes shouldContain Subtype.ZOMBIE
            tokenCard.manaCost shouldBe ManaCost.ZERO
        }
        withClue("The token is a Zombie, so Cursecloth's own lord half pumps it to 3/3") {
            driver.state.projectedState.getPower(token) shouldBe 3
            driver.state.projectedState.getToughness(token) shouldBe 3
        }
    }

    test("the grant lasts only until end of turn") {
        val driver = newDriver()
        val me = driver.player1
        val (_, bear) = grantEmbalm(driver, "Wrappings Test Bear")

        driver.state.grantedActivatedAbilities.any { it.entityId == bear }.shouldBeTrue()

        driver.passPriorityUntil(Step.UPKEEP)
        withClue("'until end of turn' — cleanup drops the granted ability") {
            driver.state.grantedActivatedAbilities.any { it.entityId == bear }.shouldBeFalse()
        }
    }

    test("a noncreature card in the graveyard is not a legal target") {
        val driver = newDriver()
        val me = driver.player1
        val wrappings = driver.putPermanentOnBattlefield(me, "Cursecloth Wrappings")
        val bolt = driver.putCardInGraveyard(me, "Wrappings Test Bolt")
        val bear = driver.putCardInGraveyard(me, "Wrappings Test Bear")

        val legalTargets = driver.legalActions(me)
            .filter { (it.action as? ActivateAbility)?.sourceId == wrappings }
            .flatMap { it.validTargets.orEmpty() }

        withClue("Only the creature card is offered — 'target creature card in your graveyard'") {
            legalTargets shouldContain bear
            legalTargets shouldNotContain bolt
        }
    }
})
