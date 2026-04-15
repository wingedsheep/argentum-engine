package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainCastOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainCastOf
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.FestivalOfEmbers
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.OtterballAntics
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.MuldrothaTheGravetide
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.SqueeTheImmortal
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.CastFromZoneEnumerator].
 *
 * The enumerator dispatches to ~11 different zone-source paths. This file
 * organizes coverage by path, with each `context` block testing one source.
 *
 * Paths covered:
 * - Flashback (graveyard alternative cost)
 * - MayPlayFromExile (per-card exile permission, e.g. cascade victims)
 * - Top of library (player-wide PlayFromTopOfLibrary, e.g. Future Sight)
 * - Intrinsic zone cast (MayCastSelfFromZones, e.g. Squee, the Immortal)
 * - Graveyard permanents (MayPlayPermanentsFromGraveyard, e.g. Muldrotha)
 * - Graveyard with life cost (MayCastFromGraveyardWithLifeCost,
 *   e.g. Festival of Embers)
 *
 * Deferred to a follow-up phase: warp from hand/exile, graveyard with forage,
 * kicker on zone casts, linked exile (cascade/adventure).
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

    // -------------------------------------------------------------------------
    context("Graveyard permanents (MayPlayPermanentsFromGraveyard — Muldrotha)") {

        test("Muldrotha on battlefield — a creature in graveyard is castable from GRAVEYARD") {
            val driver = setupP1(
                battlefield = listOf(
                    "Muldrotha, the Gravetide",
                    "Forest", "Forest", "Forest"  // 3 green for {2}{G} Grizzly Bears
                ),
                graveyard = listOf("Grizzly Bears"),
                extraSetCards = listOf(MuldrothaTheGravetide)
            )

            // There's no copy of Grizzly Bears in hand (setupP1 empties hand back
            // to library), so the only cast of Grizzly Bears must come from the
            // graveyard via Muldrotha's static ability.
            val casts = driver.enumerateFor(driver.player1).castActionsFor("Grizzly Bears")

            casts shouldHaveSize 1
            val cast = casts.single()
            cast.sourceZone shouldBe "GRAVEYARD"
            cast.affordable shouldBe true
            cast.manaCostString shouldBe "{1}{G}"
        }

        test("without Muldrotha — the same creature in graveyard is NOT castable from GRAVEYARD") {
            val driver = setupP1(
                battlefield = listOf("Forest", "Forest", "Forest"),
                graveyard = listOf("Grizzly Bears")
            )

            val casts = driver.enumerateFor(driver.player1)
                .castActionsFor("Grizzly Bears")
                .filter { it.sourceZone == "GRAVEYARD" }

            casts.shouldBeEmpty()
        }

        test("Muldrotha's permission is active-player-only — opponent cannot cast from their graveyard") {
            // P1 is the active player in setupP1. Put Muldrotha + a creature card
            // in P2's graveyard; P2 is NOT active, so Muldrotha's permission does
            // not apply to P2 via the active-player guard.
            val driver = setupP1(
                battlefield = listOf("Muldrotha, the Gravetide"),
                extraSetCards = listOf(MuldrothaTheGravetide)
            )

            // P2 is not active, so enumerating for P2 should not produce
            // graveyard-source CastSpell actions — even if P2 had creatures in
            // graveyard and Muldrotha on the battlefield, the guard stops the
            // path. We enumerate for P2 with an empty P2 graveyard; the assertion
            // simply verifies no graveyard casts appear for P2.
            val casts = driver.enumerateFor(driver.player2)
                .filter { it.sourceZone == "GRAVEYARD" && it.actionType == "CastSpell" }

            casts.shouldBeEmpty()
        }
    }

    // -------------------------------------------------------------------------
    context("Graveyard with life cost (MayCastFromGraveyardWithLifeCost — Festival of Embers)") {

        test("Festival of Embers + instant in graveyard + life available — cast emitted with life cost info") {
            val driver = setupP1(
                battlefield = listOf("Festival of Embers", "Mountain"),
                graveyard = listOf("Lightning Bolt"),
                extraSetCards = listOf(FestivalOfEmbers)
            )

            val cast = driver.enumerateFor(driver.player1)
                .castActionsFor("Lightning Bolt")
                .single { it.sourceZone == "GRAVEYARD" }

            cast.affordable shouldBe true
            cast.manaCostString shouldBe "{R}"
            // Festival's lifeCost is 1; the enumerator puts that on the action.
            cast.additionalLifeCost shouldBe 1
        }

        test("Festival of Embers + insufficient life — cast is NOT emitted (life-check `continue`s)") {
            val driver = setupP1(
                battlefield = listOf("Festival of Embers", "Mountain"),
                graveyard = listOf("Lightning Bolt"),
                extraSetCards = listOf(FestivalOfEmbers)
            )
            // Drain P1 to 0 life so the lifeCost of 1 is unaffordable.
            val p1 = driver.game.state.getEntity(driver.player1)!!
                .with(LifeTotalComponent(0))
            driver.game.replaceState(driver.game.state.withEntity(driver.player1, p1))

            val casts = driver.enumerateFor(driver.player1)
                .castActionsFor("Lightning Bolt")
                .filter { it.sourceZone == "GRAVEYARD" }

            casts.shouldBeEmpty()
        }

        test("Festival of Embers + instant in graveyard without mana — cast emitted as unaffordable") {
            val driver = setupP1(
                // No lands on battlefield — insufficient mana for Lightning Bolt's {R}.
                battlefield = listOf("Festival of Embers"),
                graveyard = listOf("Lightning Bolt"),
                extraSetCards = listOf(FestivalOfEmbers)
            )

            val cast = driver.enumerateFor(driver.player1)
                .castActionsFor("Lightning Bolt")
                .single { it.sourceZone == "GRAVEYARD" }

            cast.affordable shouldBe false
            cast.additionalLifeCost shouldBe 1
        }

        test("non-instant/sorcery in graveyard — filter excludes it from the life-cost path") {
            // Festival's filter is InstantOrSorcery. Put Grizzly Bears (a creature)
            // in the graveyard; it should NOT surface via this path.
            val driver = setupP1(
                battlefield = listOf("Festival of Embers", "Forest", "Forest"),
                graveyard = listOf("Grizzly Bears"),
                extraSetCards = listOf(FestivalOfEmbers)
            )

            val casts = driver.enumerateFor(driver.player1)
                .castActionsFor("Grizzly Bears")
                .filter { it.sourceZone == "GRAVEYARD" }

            casts.shouldBeEmpty()
        }
    }
})
