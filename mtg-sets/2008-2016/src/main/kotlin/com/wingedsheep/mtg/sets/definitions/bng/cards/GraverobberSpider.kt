package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Graverobber Spider
 * {3}{G}
 * Creature — Spider
 * 2/4
 *
 * Reach
 * {3}{B}: This creature gets +X/+X until end of turn, where X is the number of creature cards in
 * your graveyard. Activate only once each turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val GraverobberSpider = card("Graverobber Spider") {
    manaCost = "{3}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 4
    oracleText = "Reach\n" +
        "{3}{B}: This creature gets +X/+X until end of turn, where X is the number of creature cards " +
        "in your graveyard. Activate only once each turn."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        val x = DynamicAmounts.creatureCardsInYourGraveyard()
        effect = Effects.ModifyStats(x, x, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Richard Wright"
        flavorText = "Cloaks woven from its webs are durable and waterproof but said to bring on nightmares."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41810472-ba24-44b6-886c-f8906ff0a7df.jpg?1783939536"
    }
}
