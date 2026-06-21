package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intruding Soulrager
 * {U}{R}
 * Creature — Spirit
 * 2/2
 *
 * Vigilance
 * {T}, Sacrifice a Room: This creature deals 2 damage to each opponent. Draw a card.
 */
val IntrudingSoulrager = card("Intruding Soulrager") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "{T}, Sacrifice a Room: This creature deals 2 damage to each opponent. Draw a card."

    keywords(Keyword.VIGILANCE)

    // {T}, Sacrifice a Room: This creature deals 2 damage to each opponent. Draw a card.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Room")),
        )
        effect = Effects.Composite(
            Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Jeremy Wilson"
        flavorText = "The souls of those who perished outside the House persist as fractured echoes, " +
            "driven to share their misery with the living."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adffc298-0219-4807-ab05-d94460e767bc.jpg?1726286682"
    }
}
