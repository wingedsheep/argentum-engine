package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainCastOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainCastOf
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.OtterballAntics
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.SqueeTheImmortal
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.CastFromZoneEnumerator].
 *
 * The enumerator dispatches to ~11 different zone-source paths. This file
 * organizes coverage by path, with each `context` block testing one source.
 *
 * Paths covered in phase 4:
 * - Flashback (graveyard alternative cost)
 * - MayPlayFromExile (per-card exile permission, e.g. cascade victims)
 * - Top of library (player-wide PlayFromTopOfLibrary, e.g. Future Sight)
 * - Intrinsic zone cast (MayCastSelfFromZones, e.g. Squee, the Immortal)
 *
 * Deferred to a follow-up phase: warp from hand/exile, graveyard with forage,
 * graveyard with life cost (Festival of Embers), kicker on zone casts,
 * graveyard permanents (Muldrotha), linked exile (cascade/adventure).
 */
class CastFromZoneEnumeratorTest : FunSpec({

    // -------------------------------------------------------------------------
    context("Flashback (graveyard alternative cost)") {

        test("Otterball Antics in graveyard with sufficient mana surfaces a CastWithFlashback action") {
            val driver = setupP1(
                battlefield = listOf("Island", "Island", "Island", "Island"),
                graveyard = listOf("Otterball Antics"),
                extraSetCards = listOf(OtterballAntics)
            )

            val view = driver.enumerateFor(driver.player1)

            view shouldContainCastOf "Otterball Antics"
            val flashback = view.castActionsFor("Otterball Antics").first()
            flashback.actionType shouldBe "CastWithFlashback"
            flashback.affordable shouldBe true
            flashback.manaCostString shouldBe "{3}{U}"
            flashback.sourceZone shouldBe "GRAVEYARD"
            (flashback.action as CastSpell).useAlternativeCost shouldBe true
        }

        test("flashback action is unaffordable when insufficient mana but still listed") {
            // Only 2 Islands available — flashback {3}{U} needs 4. The enumerator
            // emits an unaffordable entry (unlike base CastSpell, which drops the
            // action entirely).
            val driver = setupP1(
                battlefield = listOf("Island", "Island"),
                graveyard = listOf("Otterball Antics"),
                extraSetCards = listOf(OtterballAntics)
            )

            val flashback = driver.enumerateFor(driver.player1)
                .castActionsFor("Otterball Antics").first()

            flashback.affordable shouldBe false
            flashback.actionType shouldBe "CastWithFlashback"
        }

        test("sorcery flashback is blocked at instant speed (upkeep step)") {
            val driver = setupP1(
                battlefield = listOf("Island", "Island", "Island", "Island"),
                graveyard = listOf("Otterball Antics"),
                extraSetCards = listOf(OtterballAntics),
                atStep = Step.UPKEEP
            )

            driver.enumerateFor(driver.player1) shouldNotContainCastOf "Otterball Antics"
        }

        test("an empty graveyard produces no flashback actions") {
            val driver = setupP1(
                battlefield = listOf("Island"),
                extraSetCards = listOf(OtterballAntics)
            )

            val flashbackActions = driver.enumerateFor(driver.player1)
                .filter { it.actionType == "CastWithFlashback" }

            flashbackActions shouldHaveSize 0
        }
    }

    // -------------------------------------------------------------------------
    context("MayPlayFromExile (per-card exile permission)") {

        test("Lightning Bolt in exile with MayPlayFromExile produces a CastSpell action") {
            val driver = setupP1(
                battlefield = listOf("Mountain"),
                exile = listOf("Lightning Bolt")
            )
            // Attach the permission to the exiled card.
            val exiledId = driver.game.state.getZone(ZoneKey(driver.player1, Zone.EXILE))
                .first { id ->
                    driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
            val withPermission = driver.game.state.getEntity(exiledId)!!
                .with(MayPlayFromExileComponent(controllerId = driver.player1))
            driver.game.replaceState(driver.game.state.withEntity(exiledId, withPermission))

            val view = driver.enumerateFor(driver.player1)

            view shouldContainCastOf "Lightning Bolt"
            val cast = view.castActionsFor("Lightning Bolt").first()
            cast.sourceZone shouldBe "EXILE"
            cast.affordable shouldBe true
        }

        test("an exiled card without the permission component is NOT castable") {
            val driver = setupP1(
                battlefield = listOf("Mountain"),
                exile = listOf("Lightning Bolt")
            )
            // No MayPlayFromExile attached.

            driver.enumerateFor(driver.player1) shouldNotContainCastOf "Lightning Bolt"
        }

        test("exile permission belonging to opponent does not authorize me to cast") {
            val driver = setupP1(
                battlefield = listOf("Mountain"),
                exile = listOf("Lightning Bolt")
            )
            val exiledId = driver.game.state.getZone(ZoneKey(driver.player1, Zone.EXILE))
                .first { id ->
                    driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
            // Attach permission for the OPPONENT, not me.
            val container = driver.game.state.getEntity(exiledId)!!
                .with(MayPlayFromExileComponent(controllerId = driver.player2))
            driver.game.replaceState(driver.game.state.withEntity(exiledId, container))

            driver.enumerateFor(driver.player1) shouldNotContainCastOf "Lightning Bolt"
        }
    }

    // -------------------------------------------------------------------------
    context("Top of library (PlayFromTopOfLibrary)") {

        test("with Future Sight in play, the top card of library is castable") {
            // Future Sight ({2}{U}{U}{U}) grants PlayFromTopOfLibrary statically.
            // We don't need to actually pay for Future Sight — placing it on the
            // battlefield via surgery is enough.
            val driver = setupP1(
                battlefield = listOf("Future Sight", "Mountain"),
                extraLibrary = listOf("Lightning Bolt")
            )

            // Move a Lightning Bolt to be the top of P1's library.
            var state = driver.game.state
            val library = ZoneKey(driver.player1, Zone.LIBRARY)
            val boltId = state.getLibrary(driver.player1).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Lightning Bolt"
            }
            state = state.removeFromZone(library, boltId)
            state = state.copy(
                zones = state.zones + (library to (listOf(boltId) + state.getZone(library)))
            )
            driver.game.replaceState(state)

            val view = driver.enumerateFor(driver.player1)

            view shouldContainCastOf "Lightning Bolt"
            val cast = view.castActionsFor("Lightning Bolt").first()
            cast.sourceZone shouldBe "LIBRARY"
        }

        test("without Future Sight, the top card of library is NOT castable") {
            val driver = setupP1(
                battlefield = listOf("Mountain")
            )
            // Top of library is a random Forest from filler — definitely not
            // Lightning Bolt. Either way, no permission means no library cast.

            val libraryCasts = driver.enumerateFor(driver.player1)
                .filter { it.sourceZone == "LIBRARY" && it.actionType == "CastSpell" }

            libraryCasts shouldHaveSize 0
        }
    }

    // -------------------------------------------------------------------------
    context("Intrinsic zone cast (MayCastSelfFromZones — Squee)") {

        test("Squee in graveyard with sufficient mana is castable from graveyard") {
            val driver = setupP1(
                battlefield = listOf("Mountain", "Mountain", "Mountain"),
                graveyard = listOf("Squee, the Immortal"),
                extraSetCards = listOf(SqueeTheImmortal)
            )

            val view = driver.enumerateFor(driver.player1)

            view shouldContainCastOf "Squee, the Immortal"
            val cast = view.castActionsFor("Squee, the Immortal").first()
            cast.sourceZone shouldBe "GRAVEYARD"
            cast.affordable shouldBe true
            cast.manaCostString shouldBe "{1}{R}{R}"
        }

        test("Squee in exile is also castable (intrinsic permission covers both zones)") {
            val driver = setupP1(
                battlefield = listOf("Mountain", "Mountain", "Mountain"),
                exile = listOf("Squee, the Immortal"),
                extraSetCards = listOf(SqueeTheImmortal)
            )

            val cast = driver.enumerateFor(driver.player1)
                .castActionsFor("Squee, the Immortal").first()

            cast.sourceZone shouldBe "EXILE"
        }

        test("Squee with no mana is shown as unaffordable") {
            val driver = setupP1(
                graveyard = listOf("Squee, the Immortal"),
                extraSetCards = listOf(SqueeTheImmortal)
            )

            val cast = driver.enumerateFor(driver.player1)
                .castActionsFor("Squee, the Immortal").first()

            cast.affordable shouldBe false
        }

        test("Squee as a creature is sorcery-speed-only — no upkeep cast") {
            val driver = setupP1(
                battlefield = listOf("Mountain", "Mountain", "Mountain"),
                graveyard = listOf("Squee, the Immortal"),
                extraSetCards = listOf(SqueeTheImmortal),
                atStep = Step.UPKEEP
            )

            val cast = driver.enumerateFor(driver.player1)
                .castActionsFor("Squee, the Immortal").firstOrNull()
            // The intrinsic zone-cast path emits an unaffordable entry when timing
            // is wrong (vs. flashback which skips entirely).
            cast?.affordable shouldBe false
        }
    }
})
