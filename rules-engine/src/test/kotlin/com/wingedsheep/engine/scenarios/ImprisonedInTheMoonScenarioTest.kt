package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.ImprisonedInTheMoon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Imprisoned in the Moon
 * {2}{U} Enchantment — Aura
 * Enchant creature, land, or planeswalker
 * Enchanted permanent is a colorless land with "{T}: Add {C}" and loses all other card types
 * and abilities.
 *
 * This is the CURRENT (2017-errata) Oracle wording, verified against Scryfall — not the original
 * 2015 templating ("When ~ enters the battlefield, if enchanted permanent isn't a land, it
 * becomes..."). The current wording has no ETB trigger and no intervening-if: it's a continuous
 * static effect tied to the Aura's presence, and it applies even when the enchanted permanent is
 * already a land (only its mana ability changes, not its land-ness). See the doc comment on
 * [ImprisonedInTheMoon] for the rulings this is built from.
 */
class ImprisonedInTheMoonScenarioTest : FunSpec({

    val manaAbilityId = ImprisonedInTheMoon.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().first().ability.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ImprisonedInTheMoon))
        return driver
    }

    test("enchanting a nonland creature turns it into a colorless land, losing creature-ness, " +
        "colors, and abilities — but keeping its creature subtypes and legendary supertype") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Test Hasty Prospector: legendary 2/1 red Monkey Pirate with Haste + a mana ability.
        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Imprisoned in the Moon")
        driver.giveMana(player, Color.BLUE, 3)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        val projected = driver.state.projectedState

        // No longer a creature; is now a land (Layer 4).
        projected.isCreature(ragavan) shouldBe false
        projected.hasType(ragavan, "LAND") shouldBe true

        // Colorless (Layer 5) — loses its printed red.
        projected.hasColor(ragavan, Color.RED) shouldBe false

        // Loses all (other) abilities (Layer 6) — Haste and its mana ability are gone.
        projected.hasLostAllAbilities(ragavan) shouldBe true
        projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe false

        // Subtypes are NOT cleared by this effect (only card types and abilities are) — per the
        // 2016-07-13 ruling that a Plains enchanted by this stays a Plains. The same applies to
        // a creature's subtypes: Ragavan keeps "Monkey"/"Pirate" even though it's no longer a
        // creature.
        projected.hasSubtype(ragavan, "Monkey") shouldBe true
        projected.hasSubtype(ragavan, "Pirate") shouldBe true

        // Still legendary — CR 205.4b: changing card types/subtypes never changes supertypes.
        projected.isLegendary(ragavan) shouldBe true
    }

    test("the granted mana ability taps the transformed permanent for {C}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Imprisoned in the Moon")
        driver.giveMana(player, Color.BLUE, 3)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        val result = driver.submit(
            ActivateAbility(playerId = player, sourceId = ragavan, abilityId = manaAbilityId)
        )
        result.isSuccess shouldBe true

        driver.isTapped(ragavan) shouldBe true
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()
        pool?.colorless shouldBe 1
    }

    test("enchanting a land also applies: it keeps its land subtype but loses its native mana " +
        "ability in favor of {T}: Add {C}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        val aura = driver.putCardInHand(player, "Imprisoned in the Moon")
        driver.giveMana(player, Color.BLUE, 3)
        driver.castSpell(player, aura, listOf(forest))
        driver.bothPass()

        val projected = driver.state.projectedState

        // Still a land, still has the "Forest" land type — per the 2016-07-13 ruling.
        projected.hasType(forest, "LAND") shouldBe true
        projected.hasSubtype(forest, "Forest") shouldBe true

        // But its intrinsic mana ability is gone — only the granted {T}: Add {C} remains.
        projected.hasLostAllAbilities(forest) shouldBe true

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
        val forestAbilities = actions.filter { (it.action as? ActivateAbility)?.sourceId == forest }
        forestAbilities.size shouldBe 1
        (forestAbilities.first().action as ActivateAbility).abilityId shouldBe manaAbilityId

        driver.submit(
            ActivateAbility(playerId = player, sourceId = forest, abilityId = manaAbilityId)
        ).isSuccess shouldBe true

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()
        pool?.colorless shouldBe 1
        pool?.green shouldBe 0
    }

    test("destroying the Aura reverts the enchanted creature to its original characteristics") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Imprisoned in the Moon")
        driver.giveMana(player, Color.BLUE, 3)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        // Sanity: the transform is active while the Aura is attached.
        driver.state.projectedState.isCreature(ragavan) shouldBe false

        // The transform comes from a static ability on the Aura permanent itself — a continuous
        // effect, not a one-shot resolution effect from an ETB trigger (the current Oracle text
        // has no ETB trigger at all). Once the Aura leaves the battlefield, the continuous effect
        // ends and every characteristic it set reverts.
        val auraOnBattlefield = driver.getPermanents(player)
            .first { driver.getCardName(it) == "Imprisoned in the Moon" }
        driver.moveToGraveyard(auraOnBattlefield)

        val projected = driver.state.projectedState
        projected.isCreature(ragavan) shouldBe true
        projected.hasType(ragavan, "LAND") shouldBe false
        projected.hasLostAllAbilities(ragavan) shouldBe false
        projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe true
        projected.hasColor(ragavan, Color.RED) shouldBe true
    }
})
