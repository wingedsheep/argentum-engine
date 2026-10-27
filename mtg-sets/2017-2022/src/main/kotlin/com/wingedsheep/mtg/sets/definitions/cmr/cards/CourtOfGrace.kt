package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Court of Grace — Commander Legends #16
 * {2}{W}{W} · Enchantment
 * When this enchantment enters, you become the monarch.
 * At the beginning of your upkeep, create a 1/1 white Spirit creature token with flying. If you're
 * the monarch, create a 4/4 white Angel creature token with flying instead.
 *
 * The upkeep trigger always triggers; "if you're the monarch … instead" is not an intervening-if but
 * a replacement checked as the ability resolves, so it is an [Effects.If] choosing between the two
 * token creations at resolution.
 */
val CourtOfGrace = card("Court of Grace") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you become the monarch.\n" +
        "At the beginning of your upkeep, create a 1/1 white Spirit creature token with flying. " +
        "If you're the monarch, create a 4/4 white Angel creature token with flying instead."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.BecomeMonarch()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.YouAreTheMonarch,
            then = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/f/7/f7c65b6a-9c63-4604-b7db-d75772799616.jpg?1783928590",
            ),
            otherwise = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/2/1/21e580cb-6092-48b7-ba3e-d64ec28860da.jpg?1783928591",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Denman Rooke"
        flavorText = "A righteous sovereign makes for a blessed realm."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02f0be1b-554e-4b61-b495-6e2a7369cdb1.jpg?1783928885"
        ruling("2020-11-10", "The game starts with no monarch. Once an effect makes one player the monarch, the game will have exactly one monarch from that point forward. As a player becomes the monarch, the current monarch (if any) ceases being the monarch.")
        ruling("2020-11-10", "If the monarch leaves the game during another player's turn, that player becomes the monarch. If the monarch leaves the game during their turn, the next player in turn order becomes the monarch.")
    }
}
