package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Get Lost — LCI #14
 * {1}{W} Instant — Rare
 *
 * "Destroy target creature, enchantment, or planeswalker. Its controller creates two Map tokens.
 *  (They're artifacts with "{1}, {T}, Sacrifice this token: Target creature you control explores.
 *  Activate only as a sorcery.")"
 *
 * Target filter: There is no pre-built TargetFilter for "creature, enchantment, or planeswalker",
 * so we construct one inline using CardPredicate.Or. The token-creation step uses
 * CreatePredefinedTokenEffect directly (bypassing the Effects.CreateMapToken facade) because only
 * the data-class constructor exposes the [EffectTarget.TargetController] controller override that
 * redirects the token creation to the destroyed permanent's controller rather than the caster.
 */
private val creatureEnchantmentOrPlaneswalker = TargetFilter(
    GameObjectFilter(
        cardPredicates = listOf(
            CardPredicate.Or(
                listOf(CardPredicate.IsCreature, CardPredicate.IsEnchantment, CardPredicate.IsPlaneswalker)
            )
        )
    )
)

val GetLost = card("Get Lost") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature, enchantment, or planeswalker. Its controller creates two Map tokens. " +
        "(They're artifacts with \"{1}, {T}, Sacrifice this token: Target creature you control explores. " +
        "Activate only as a sorcery.\")"

    spell {
        val t = target("target creature, enchantment, or planeswalker", TargetPermanent(filter = creatureEnchantmentOrPlaneswalker))
        effect = Effects.Composite(
            Effects.Destroy(t),
            CreatePredefinedTokenEffect("Map", 2, EffectTarget.TargetController)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/522aa72b-2b8c-484c-872b-f082101cee35.jpg?1782694599"
    }
}
