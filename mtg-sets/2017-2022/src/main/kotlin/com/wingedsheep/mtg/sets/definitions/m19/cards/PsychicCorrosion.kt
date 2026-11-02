package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psychic Corrosion
 * {2}{U}
 * Enchantment
 * Whenever you draw a card, each opponent mills two cards.
 */
val PsychicCorrosion = card("Psychic Corrosion") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you draw a card, each opponent mills two cards."

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever you draw a card, each opponent mills two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Bastien L. Deharme"
        flavorText = "\"To break another's mind is to deliver a fate worse than death. It is a terrifying power.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/285d22fa-1623-463a-83c0-a9aa7c969ce2.jpg?1783934583"
    }
}
