package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Spire Tracer — Gatecrash #135
 * {G} · Creature — Elf Scout · 1/1
 *
 * This creature can't be blocked except by creatures with flying or reach.
 */
val SpireTracer = card("Spire Tracer") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 1
    oracleText = "This creature can't be blocked except by creatures with flying or reach."

    staticAbility {
        ability = CantBeBlockedExceptBy(
            blockerFilter = GameObjectFilter.Creature.withCardPredicate(
                CardPredicate.Or(listOf(CardPredicate.HasKeyword(Keyword.FLYING), CardPredicate.HasKeyword(Keyword.REACH)))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Christopher Moeller"
        flavorText = "Cartographers on Ravnica chart not just the horizontal, but also the vertical."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/428b0d43-94c9-4f7f-b042-ea63f88ac697.jpg?1783940115"
    }
}
