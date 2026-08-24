package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deepwood Drummer
 * {1}{G}
 * Creature — Human Spellshaper
 * 1 / 1
 * {G}, {T}, Discard a card: Target creature gets +2/+2 until end of turn.
 */
val DeepwoodDrummer = card("Deepwood Drummer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{G}, {T}, Discard a card: Target creature gets +2/+2 until end of turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(2, 2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Ron Spears"
        flavorText = "His drums echo Deepwood's heartbeat."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acbed0f5-2ac0-48d8-b5ab-b4cd7176fde2.jpg"
    }
}
