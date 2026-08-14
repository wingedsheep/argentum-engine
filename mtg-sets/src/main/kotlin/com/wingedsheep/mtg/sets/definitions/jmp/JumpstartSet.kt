package com.wingedsheep.mtg.sets.definitions.jmp

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Jumpstart
 *
 * Set Code: JMP
 */
object JumpstartSet : MtgSet {

    override val code = "JMP"
    override val displayName = "Jumpstart"
    override val releaseDate = "2020-07-17"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Our Scryfall sync returns no `tjmp` rows, so Jumpstart's token art is self-hosted under
     * `web-client/public/images/tokens/` and declared here.
     *
     * Jumpstart printed the Dog token four times, with four different illustrations — so there are
     * four rows, identical but for the art. A batch of tokens created at once is dealt out of the
     * matching rows in order, which makes Release the Dogs put all four Dogs on the battlefield
     * looking like four different dogs. See [TokenPrinting.allMatches].
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Dog",
            imageUri = "/images/tokens/jmp-dog1.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Dog",
            imageUri = "/images/tokens/jmp-dog2.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Dog",
            imageUri = "/images/tokens/jmp-dog3.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Dog",
            imageUri = "/images/tokens/jmp-dog4.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(name = "Treasure", imageUri = "/images/tokens/jmp-treasure.jpeg"),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.jmp.cards"
}
