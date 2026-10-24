package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reconstruct History — Strixhaven: School of Mages #222 (canonical printing)
 * {2}{R}{W} · Sorcery
 *
 * Return up to one target artifact card, up to one target enchantment card, up to one target
 * instant card, up to one target sorcery card, and up to one target planeswalker card from your
 * graveyard to your hand.
 * Exile Reconstruct History.
 *
 * The Rise from the Wreck shape: five independent "up to one" graveyard targets, each returned
 * with its own [Effects.ReturnToHand], then `selfExile()` sends the spell to exile instead of the
 * graveyard.
 */
val ReconstructHistory = card("Reconstruct History") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Return up to one target artifact card, up to one target enchantment card, up to " +
        "one target instant card, up to one target sorcery card, and up to one target planeswalker " +
        "card from your graveyard to your hand.\n" +
        "Exile Reconstruct History."

    spell {
        val artifact = target(TargetFilter.ArtifactInYourGraveyard, optional = true)
        val enchantment = target(
            TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        val instant = target(
            TargetFilter(GameObjectFilter.Instant.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        val sorcery = target(
            TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        val planeswalker = target(
            TargetFilter(GameObjectFilter.Planeswalker.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.ReturnToHand(artifact) then
            Effects.ReturnToHand(enchantment) then
            Effects.ReturnToHand(instant) then
            Effects.ReturnToHand(sorcery) then
            Effects.ReturnToHand(planeswalker)
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e565e38-43d5-482e-a911-ce525dfee74f.jpg?1783927298"
    }
}
