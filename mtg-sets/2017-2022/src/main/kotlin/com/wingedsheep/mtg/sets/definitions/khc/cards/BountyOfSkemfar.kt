package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Bounty of Skemfar
 * {2}{G}
 * Sorcery
 * Reveal the top six cards of your library. You may put up to one land card from among them onto
 * the battlefield tapped and up to one Elf card from among them into your hand. Put the rest on the
 * bottom of your library in a random order.
 *
 * Two independent up-to-one picks over the revealed six — a land first, then an Elf from what's
 * left (the Relentless Pursuit shape) — so a card that is both a land and an Elf can fill either
 * slot but never both.
 */
val BountyOfSkemfar = card("Bounty of Skemfar") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top six cards of your library. You may put up to one land card from among them " +
        "onto the battlefield tapped and up to one Elf card from among them into your hand. Put the rest on " +
        "the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(6), revealed = true)
            val (land, afterLand) = chooseUpToSplit(
                1, from = revealed,
                filter = GameObjectFilter.Land,
                prompt = "You may put a land card onto the battlefield tapped",
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Leave",
                showAllCards = true
            )
            val (elf, rest) = chooseUpToSplit(
                1, from = afterLand,
                filter = GameObjectFilter.Any.withSubtype(Subtype.ELF),
                prompt = "You may put an Elf card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on the bottom in a random order",
                showAllCards = true
            )
            move(land, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toHand(elf)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Colin Boyer"
        flavorText = "Even war cannot dull Skemfar's beauty."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3185a67e-648c-48a8-9aad-180e1ca0f4ae.jpg?1783928336"
    }
}
