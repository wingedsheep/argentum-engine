package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Rile = card("Rile") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rile deals 1 damage to target creature you control. That creature gains trample until end of turn.\nDraw a card."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.DealDamage(1, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Igor Kieryluk"
        flavorText = "The enormous can still be at the mercy of the small."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80925750-6c90-42d1-9525-27f1f0313398.jpg?1783935739"
        ruling("2017-09-29", "If Rile targets a creature with 1 toughness, that creature won't be destroyed until after you've drawn a card. Its abilities may affect that draw or trigger on that draw if appropriate.")
        ruling("2017-09-29", "If the damage that would be dealt by Rile is prevented, the creature still gains trample until end of turn.")
        ruling("2017-09-29", "If the target creature is an illegal target by the time Rile resolves, the entire spell doesn't resolve. You won't draw a card.")
    }
}
