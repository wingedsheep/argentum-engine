package com.wingedsheep.mtg.sets.definitions.stx

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.DedicatedSlot
import com.wingedsheep.sdk.limited.DedicatedSlotBooster
import com.wingedsheep.sdk.limited.SheetSelector
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Strixhaven: School of Mages (2021)
 *
 * The set built around the five mage colleges of Strixhaven University, featuring
 * the Learn / Lesson mechanic, Magecraft, and the Mystical Archive reprints.
 *
 * Set Code: STX
 * Release Date: April 23, 2021
 */
object StrixhavenSchoolOfMagesSet : MtgSet {

    override val code = "STX"
    override val displayName = "Strixhaven: School of Mages"
    override val releaseDate = "2021-04-23"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Draft Booster (MTGJSON `booster.draft`): 9 non-Lesson commons, 3 uncommons, a non-Lesson rare or
     * mythic, one Lesson and one Mystical Archive (STA) card.
     *
     * - **Lesson slot** — replaces the basic land: one of the 15 common, rare and mythic Lessons, weighted
     *   137 per common, 18 per rare and 9 for the mythic (a rare or mythic Lesson in 99 packs of 1332). It
     *   is an extra card, not the pack's rare. The five uncommon Lessons stay on the ordinary uncommon
     *   sheet, so they open in the uncommon slots like any other uncommon.
     * - **Rare slot** — the rare sheet holds no Lessons; 60 of its 444 draws are mythic.
     * - **Mystical Archive slot — opens a tenth common instead.** The Archive cards are STA printings, not
     *   STX cards, and STA isn't a set here (13 of its 63 cards aren't implemented anywhere), so the slot
     *   draws from the STX common sheet to keep the 15-card pack and the 15 picks per draft round.
     *
     * The foil (with showcase) replacing a common in a third of packs is cosmetic and not modelled.
     */
    override val boosterStrategy: BoosterStrategy = DedicatedSlotBooster(
        base = StandardBooster(commons = 10, uncommons = 3, mythicChance = 60.0 / 444),
        slots = listOf(
            DedicatedSlot(
                SheetSelector.HasSubtype(Subtype.LESSON),
                mapOf(Rarity.COMMON to 9.0 * 137, Rarity.RARE to 5.0 * 18, Rarity.MYTHIC to 9.0),
            ),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.stx.cards"
}
