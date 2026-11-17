package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shifting Loyalties — Fate Reforged #51
 * {5}{U} · Sorcery · Uncommon
 *
 * Exchange control of two target permanents that share a card type.
 *
 * Same shape as The Trickster-God's Heist chapter II: "share a card type" is dependent target
 * selection, so the second slot's filter reads the first through `sharingCardTypeWith(first)` (any
 * card type, projected — a creature and an artifact creature pair up), and a first choice with no
 * possible partner is not offered. Both targets are re-checked on resolution: if either is illegal
 * (including because they no longer share a card type) the exchange doesn't happen. The control
 * change is a permanent override, and nothing happens if one player controls both.
 */
val ShiftingLoyalties = card("Shifting Loyalties") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exchange control of two target permanents that share a card type. " +
        "(Artifact, creature, enchantment, land, and planeswalker are card types.)"

    spell {
        val first = target(TargetFilter(GameObjectFilter.Permanent))
        val second = target(TargetFilter(GameObjectFilter.Permanent.sharingCardTypeWith(first)))
        effect = Effects.ExchangeControl(first, second)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "James Ryman"
        flavorText = "\"Show the enemy the true path, and the spirit will yearn to follow it.\"\n—Houn, Jeskai elder"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed9ff175-2e4c-4658-813e-1bfb4b9a6ca0.jpg?1783938702"

        ruling("2014-11-24", "If one of the target permanents is an illegal target when Shifting Loyalties resolves, the exchange won't happen. If both permanents are illegal targets (perhaps because they no longer share a card type), Shifting Loyalties won't resolve.")
        ruling("2014-11-24", "Either target can have card types the other does not, as long as they share at least one card type. For example, you could target a creature and an artifact creature.")
        ruling("2014-11-24", "You don't have to control either target permanent.")
        ruling("2014-11-24", "If the same player controls both permanents when Shifting Loyalties resolves, nothing happens.")
        ruling("2014-11-24", "Gaining control of an Aura or Equipment doesn't cause it to move, though gaining control of an Equipment will allow you to activate its equip ability later to attach it to a creature you control.")
    }
}
