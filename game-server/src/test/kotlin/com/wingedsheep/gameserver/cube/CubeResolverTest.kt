package com.wingedsheep.gameserver.cube

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.PrintingRef
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class CubeResolverTest : FunSpec({

    fun card(name: String, oracleId: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
    ).copy(oracleId = oracleId)

    val grizzly = card("Grizzly Bears", "grizzly-oracle")
    val runeclaw = card("Runeclaw Bear", "runeclaw-oracle")

    fun resolver(printings: List<Printing> = emptyList()) = CubeResolver(
        cardRegistry = CardRegistry().apply { register(listOf(grizzly, runeclaw)) },
        printingRegistry = PrintingRegistry().apply { register(printings) },
    )

    test("resolves names, expands counts, and preserves cube settings") {
        val result = resolver().resolve(
            CubeList(
                name = "Bear Cube",
                cards = listOf(
                    CubeCardEntry("Grizzly Bears", count = 2),
                    CubeCardEntry("Runeclaw Bear"),
                ),
                basicLandSetCode = "BLB",
                packSize = 12,
            )
        ) as CubeResolution.Success

        result.cube.name shouldBe "Bear Cube"
        result.cube.cards.map { it.name } shouldContainExactly
            listOf("Grizzly Bears", "Grizzly Bears", "Runeclaw Bear")
        result.cube.basicLandSetCode shouldBe "BLB"
        result.cube.packSize shouldBe 12
    }

    test("applies a pinned printing without changing oracle identity") {
        val printing = Printing(
            oracleId = "grizzly-oracle",
            name = "Grizzly Bears",
            setCode = "10E",
            collectorNumber = "268",
            imageUri = "https://example.test/bear.jpg",
        )

        val result = resolver(listOf(printing)).resolve(
            CubeList(
                name = "Art Cube",
                cards = listOf(
                    CubeCardEntry("Grizzly Bears", printing = printing.ref),
                ),
                basicLandSetCode = "BLB",
            )
        ) as CubeResolution.Success

        result.cube.cards.single().let {
            it.oracleId shouldBe grizzly.oracleId
            it.setCode shouldBe "10E"
            it.metadata.collectorNumber shouldBe "268"
            it.metadata.imageUri shouldBe "https://example.test/bear.jpg"
        }
    }

    test("reports every unresolvable card instead of returning a partial cube") {
        val result = resolver().resolve(
            CubeList(
                name = "Incomplete Cube",
                cards = listOf(
                    CubeCardEntry("Missing One"),
                    CubeCardEntry("Grizzly Bears"),
                    CubeCardEntry("Missing Two", count = 4),
                ),
                basicLandSetCode = "BLB",
            )
        ) as CubeResolution.Failure

        result.unresolved shouldContainExactly listOf(
            UnresolvedCubeCard("Missing One", "Card is not implemented"),
            UnresolvedCubeCard("Missing Two", "Card is not implemented"),
        )
    }

    test("reports unavailable and mismatched pinned printings") {
        val runeclawPrinting = Printing(
            oracleId = "runeclaw-oracle",
            name = "Runeclaw Bear",
            setCode = "M10",
            collectorNumber = "211",
        )
        val result = resolver(listOf(runeclawPrinting)).resolve(
            CubeList(
                name = "Bad Pins",
                cards = listOf(
                    CubeCardEntry("Grizzly Bears", printing = PrintingRef("M10", "999")),
                    CubeCardEntry("Grizzly Bears", printing = runeclawPrinting.ref),
                ),
                basicLandSetCode = "BLB",
            )
        ) as CubeResolution.Failure

        result.unresolved shouldContainExactly listOf(
            UnresolvedCubeCard(
                "Grizzly Bears",
                "Printing M10-999 is not available",
            ),
            UnresolvedCubeCard(
                "Grizzly Bears",
                "Printing M10-211 belongs to Runeclaw Bear",
            ),
        )
    }
})
