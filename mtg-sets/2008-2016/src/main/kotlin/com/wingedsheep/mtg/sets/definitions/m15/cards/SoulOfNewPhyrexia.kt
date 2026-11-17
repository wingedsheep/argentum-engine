package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Permanents you control gain indestructible until end of turn. */
private val permanentsGainIndestructible = Effects.ForEachInGroup(
    GroupFilter(GameObjectFilter.Permanent.youControl()),
    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
)

/**
 * Soul of New Phyrexia
 * {6}
 * Artifact Creature — Phyrexian Avatar
 * 6/6
 * Trample
 * {5}: Permanents you control gain indestructible until end of turn.
 * {5}, Exile this card from your graveyard: Permanents you control gain indestructible until end of turn.
 *
 * Both abilities share one effect; the second is activated from the graveyard with
 * exiling this card as part of its cost.
 */
val SoulOfNewPhyrexia = card("Soul of New Phyrexia") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Phyrexian Avatar"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "{5}: Permanents you control gain indestructible until end of turn.\n" +
        "{5}, Exile this card from your graveyard: Permanents you control gain indestructible until end of turn."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{5}")
        effect = permanentsGainIndestructible
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.ExileSelf)
        effect = permanentsGainIndestructible
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4d37dec-98d6-4815-8f3e-7de26817990c.jpg"
    }
}
