package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sparkhunter Masticore
 * {3}
 * Artifact Creature — Masticore
 * 3/4
 * As an additional cost to cast this spell, discard a card.
 * Protection from planeswalkers
 * {1}: This creature deals 1 damage to target planeswalker.
 * {3}: This creature gains indestructible until end of turn.
 *
 * The Lesser Masticore shape (discard additional cost + pinging activation). "Protection from
 * planeswalkers" is [KeywordAbility.Protection] with a card-type scope — the same projection the
 * Mirrodin "protection from artifacts" cards use, so planeswalker sources can't target it or damage it.
 */
val SparkhunterMasticore = card("Sparkhunter Masticore") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Masticore"
    power = 3
    toughness = 4
    oracleText = "As an additional cost to cast this spell, discard a card.\n" +
        "Protection from planeswalkers\n" +
        "{1}: This creature deals 1 damage to target planeswalker.\n" +
        "{3}: This creature gains indestructible until end of turn."

    additionalCost(Costs.additional.DiscardCards(1))

    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Planeswalker")))

    activatedAbility {
        cost = Costs.Mana("{1}")
        val t = target(TargetFilter.Planeswalker)
        effect = Effects.DealDamage(1, t)
        description = "{1}: This creature deals 1 damage to target planeswalker."
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        description = "{3}: This creature gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad357ed4-b4f2-45b5-b7c4-3c6013a4ea3d.jpg?1783930654"
    }
}
