package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fraying Sanity
 * {2}{U}
 * Enchantment — Aura Curse
 * Enchant player
 * At the beginning of each end step, enchanted player mills X cards, where X is the number of cards
 * put into their graveyard from anywhere this turn.
 *
 * X is turn history, read when the ability resolves: every card that reached the enchanted
 * player's graveyard this turn counts, from any zone and even if it has since left (tokens aren't
 * cards and don't).
 */
val FrayingSanity = card("Fraying Sanity") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "At the beginning of each end step, enchanted player mills X cards, where X is the number of " +
        "cards put into their graveyard from anywhere this turn."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Patterns.Library.mill(
            DynamicAmounts.cardsPutIntoGraveyardThisTurn(Player.EnchantedPlayer),
            EffectTarget.PlayerRef(Player.EnchantedPlayer),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Ryan Alexander Lee"
        flavorText = "Those wound tightest are first to unravel."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79e5c1ab-eebd-41ca-a1ca-29b00370f6e8.jpg?1783936053"
    }
}
