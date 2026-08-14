package com.wingedsheep.engine.registry

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.handlers.effects.permanent.types.buildCardComponentForDfcFace
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.definitions.isd.cards.GarrukRelentless
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.mtg.sets.tokens.TokenCreationSites
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.PrintingRef
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A reprint has to keep wearing its own art — on both faces, and on the tokens it makes.
 *
 * Garruk Relentless is the card that exposed both halves of this. Played out of an Innistrad
 * Remastered pool he transformed into an *Innistrad* Garruk, the Veil-Cursed, and the Wolf he made
 * showed no art at all. Two independent causes, one theme: the printing the player brought was
 * being dropped in favour of the card's earliest printing.
 *
 * 1. The face swap rebuilt the `CardComponent` from `face.metadata.imageUri` — the canonical
 *    definition's art — instead of the back-face art the printing stamped on the entity.
 * 2. Token art keys off a set, and the only set the token executors could see was the one encoded
 *    in `cardDefinitionId`, which deliberately names the *oracle* definition (`#ISD-181`). The
 *    pinned reprint had nowhere to live until `CardComponent.printingSetCode`.
 *
 * (Garruk's own dead `imageUri` override — a Scryfall id that 404s — was the reason the Wolf came
 * out blank rather than merely wearing the wrong set's art. `TokenArtCoverageTest` guards the
 * corpus against art that resolves to nothing; this guards art that resolves to the wrong set.)
 */
class DfcReprintArtTest : FunSpec({

    val inrFront = "https://cards.scryfall.io/normal/front/6/8/6897514f-e396-46d6-91e3-158366c741bb.jpg?1783908104"
    val inrBack = "https://cards.scryfall.io/normal/back/6/8/6897514f-e396-46d6-91e3-158366c741bb.jpg?1783908104"
    val inrBlackWolf = "https://cards.scryfall.io/normal/front/f/5/f5561f57-34e2-4c01-8094-0ecb101c7fa1.jpg?1783907968"
    val isdBlackWolf = "https://cards.scryfall.io/normal/front/7/a/7a49607c-427a-474c-ad77-60cd05844b3c.jpg?1783940882"

    val printingRegistry = PrintingRegistry().apply {
        MtgSetCatalog.all.forEach { register(it.printings) }
        MtgSetCatalog.all.forEach { set -> set.cards.forEach { registerSynthesizedDefault(it) } }
    }

    val tokenArtRegistry = TokenArtRegistry().apply {
        for (set in MtgSetCatalog.all) {
            register(set.code, TokenArtData.forSet(set), set.cards.map { it.name })
        }
    }

    // A set's definitions are stamped with their set code when the registry is loaded
    // (`GameBeansConfig.stamp`), not in `mtg-sets` — so a definition read straight out of the
    // package, as here, has to be stamped to stand in for the one a real game holds.
    val garruk = GarrukRelentless.copy(setCode = "ISD")

    /** Garruk as an Innistrad Remastered pool would mint him: INR art, ISD oracle identity. */
    fun remasteredGarruk(): CardComponent =
        CardEntityFactory.create(
            cardDef = garruk,
            ownerId = EntityId("player-1"),
            printingRef = PrintingRef("INR", "197"),
            printingRegistry = printingRegistry,
        ).get<CardComponent>()!!

    test("a reprint's back face shows the reprint's art, and flipping back restores its front") {
        val front = remasteredGarruk()
        front.imageUri shouldBe inrFront
        front.backFaceImageUri shouldBe inrBack

        val back = buildCardComponentForDfcFace(front, garruk.backFace!!)
        back.name shouldBe "Garruk, the Veil-Cursed"
        back.imageUri shouldBe inrBack
        // The other face is always the one image the card isn't currently showing, so a second
        // flip has the front art to go back to.
        back.backFaceImageUri shouldBe inrFront

        val flippedBack = buildCardComponentForDfcFace(back, garruk)
        flippedBack.imageUri shouldBe inrFront
        flippedBack.backFaceImageUri shouldBe inrBack
    }

    test("the printing survives the flip, so the back face still mints the reprint's tokens") {
        val front = remasteredGarruk()
        front.printingSetCode shouldBe "INR"
        // `cardDefinitionId` names the oracle definition on purpose — which is exactly why it can't
        // be the source of token art for a reprint.
        front.cardDefinitionId shouldBe "Garruk Relentless#ISD-181"

        val back = buildCardComponentForDfcFace(front, garruk.backFace!!)
        back.printingSetCode shouldBe "INR"
        back.originalSetCode shouldBe "ISD"
    }

    test("the 1/1 black deathtouch Wolf resolves to the art of the set that printed the Garruk") {
        fun wolfArt(printingSetCode: String?) = tokenArtRegistry.resolve(
            sourceCardDefinitionId = "Garruk, the Veil-Cursed",
            tokenName = "Wolf",
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            sourcePrintingSetCode = printingSetCode,
        )

        wolfArt("INR") shouldBe inrBlackWolf
        wolfArt("ISD") shouldBe isdBlackWolf
        // Without the printing there is nothing to key on: the back face's name is not a card name
        // any set registers, so this is the null the executors fall through on.
        wolfArt(null) shouldBe null
    }

    test("neither Wolf pins art onto the script, so the registry is what answers") {
        // Walks the serialised card tree, so this covers the back face's Wolf too.
        val sites = TokenCreationSites.of(GarrukRelentless)
        sites.map { it.tokenName } shouldBe listOf("Wolf", "Wolf")
        sites.mapNotNull { it.explicitImageUri } shouldBe emptyList()
    }
})
