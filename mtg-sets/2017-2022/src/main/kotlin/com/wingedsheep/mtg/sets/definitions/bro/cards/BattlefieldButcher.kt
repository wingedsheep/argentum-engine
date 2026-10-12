package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Battlefield Butcher — The Brothers' War #86
 * {2}{B} · Creature — Human Soldier · 1/4 · Uncommon
 *
 * {5}, {T}: Each opponent loses 2 life. This ability costs {1} less to activate for each
 * creature card in your graveyard.
 *
 * The discount rides `genericCostReduction` (Voldaren Estate's shape), so only the generic {5}
 * shrinks and it can't go below zero.
 */
val BattlefieldButcher = card("Battlefield Butcher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 4
    oracleText = "{5}, {T}: Each opponent loses 2 life. This ability costs {1} less to activate " +
        "for each creature card in your graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), AbilityCost.Tap)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
        genericCostReduction = DynamicAmounts.creatureCardsInYourGraveyard()
        description = "{5}, {T}: Each opponent loses 2 life. This ability costs {1} less to activate " +
            "for each creature card in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Lucas Graciano"
        flavorText = "\"Any more giant metal monsters want to try their luck? I'm just getting warmed up!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90c47e31-d4f6-46f3-94b7-1726c4a4a8eb.jpg?1783920096"
    }
}
