package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vraska, Relic Seeker
 * {4}{B}{G}
 * Legendary Planeswalker — Vraska
 * Starting Loyalty: 6
 *
 * +2: Create a 2/2 black Pirate creature token with menace.
 * −3: Destroy target artifact, creature, or enchantment. Create a Treasure token.
 * −10: Target player's life total becomes 1.
 *
 * - The −3's Treasure is created even if the target survives (indestructible); if the target is
 *   illegal on resolution the whole ability fizzles, so no Treasure (ruling).
 * - The −10 is `SetLifeTotal(1)`, which the executor applies as life loss (or gain), so life-loss
 *   locks and "lost life this turn" bookkeeping see it (ruling).
 * - The Pirate token's art comes from the synced XLN token sheet, as on Fathom Fleet Captain.
 */
val VraskaRelicSeeker = card("Vraska, Relic Seeker") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 6
    oracleText = "+2: Create a 2/2 black Pirate creature token with menace.\n" +
        "−3: Destroy target artifact, creature, or enchantment. Create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "−10: Target player's life total becomes 1."

    // +2: Create a 2/2 black Pirate creature token with menace.
    loyaltyAbility(+2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Pirate"),
            keywords = setOf(Keyword.MENACE),
        )
    }

    // −3: Destroy target artifact, creature, or enchantment. Create a Treasure token.
    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment)
        effect = Effects.Destroy(permanent) then Effects.CreateTreasure()
    }

    // −10: Target player's life total becomes 1.
    loyaltyAbility(-10) {
        val player = target(Targets.Player)
        effect = Effects.SetLifeTotal(1, player)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "232"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/967b7272-33a6-46e9-b0aa-4757aee54b3a.jpg?1783935707"
        ruling(
            "2017-09-29",
            "If the target artifact, creature, or enchantment is an illegal target by the time Vraska's second ability resolves, the entire ability doesn't resolve. You won't get a Treasure. If, on the other hand, the target is a legal target but isn't destroyed (most likely because it has indestructible), you will get a Treasure.",
        )
        ruling(
            "2017-09-29",
            "For a player's life total to become 1, what actually happens is that the player loses the appropriate amount of life (or in some rare cases, gains the appropriate amount of life). For example, if the targeted player's life total is 4 when this ability resolves, that player loses 3 life. Other cards that interact with life loss will interact with this effect accordingly.",
        )
    }
}
