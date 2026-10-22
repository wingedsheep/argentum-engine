package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mysterious Tome // Chilling Chronicle (Innistrad: Midnight Hunt)
 * {2}{U}
 * Artifact — Book // Artifact — Book
 *
 * Front — Mysterious Tome
 *   {2}, {T}: Draw a card. Transform this artifact.
 *
 * Back — Chilling Chronicle (blue color indicator)
 *   {1}, {T}: Tap target nonland permanent. Transform this artifact.
 */

private val MysteriousTomeFront = card("Mysterious Tome") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Book"
    oracleText = "{2}, {T}: Draw a card. Transform this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.DrawCards(1) then Effects.Transform(EffectTarget.Self)
        description = "{2}, {T}: Draw a card. Transform this artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "David Auden Nash"
        flavorText = "The villagers pored through the old storybook, hoping that one of the tales " +
            "within might hold some clue to saving the fading sun."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caa57b63-bb11-45e8-8795-de92ca61f4f1.jpg?1783925645"
    }
}

private val ChillingChronicle = card("Chilling Chronicle") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Artifact — Book"
    oracleText = "{1}, {T}: Tap target nonland permanent. Transform this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.Tap(t) then Effects.Transform(EffectTarget.Self)
        description = "{1}, {T}: Tap target nonland permanent. Transform this artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "David Auden Nash"
        flavorText = "But every story ended the same way: the countryside was captured in the icy " +
            "grip of winter and darkened by eternal night."
        imageUri = "https://cards.scryfall.io/normal/back/c/a/caa57b63-bb11-45e8-8795-de92ca61f4f1.jpg?1783925645"
    }
}

val MysteriousTome: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = MysteriousTomeFront,
    backFace = ChillingChronicle,
)
