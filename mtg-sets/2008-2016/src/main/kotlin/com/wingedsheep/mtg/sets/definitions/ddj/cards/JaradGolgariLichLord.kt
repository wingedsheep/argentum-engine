package com.wingedsheep.mtg.sets.definitions.ddj.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jarad, Golgari Lich Lord — Duel Decks: Izzet vs. Golgari #45 (earliest printing; reprinted in
 * Return to Ravnica, Commander 2015, Commander Anthology, GRN Guild Kit, Tarkir: Dragonstorm Commander)
 * {B}{B}{G}{G} · Legendary Creature — Zombie Elf · 2/2
 *
 * Jarad gets +1/+1 for each creature card in your graveyard.
 * {1}{B}{G}, Sacrifice another creature: Each opponent loses life equal to the sacrificed creature's power.
 * Sacrifice a Swamp and a Forest: Return this card from your graveyard to your hand.
 *
 * The last ability is a composite of two sacrifice atoms, paid by one disjoint assignment — a single
 * Swamp Forest can pay either atom but never both (the 2012-10-01 ruling); the engine asks for each
 * land in turn and offers only lands that leave the other atom payable. The life loss reads the
 * sacrificed creature's last-known power ([DynamicAmounts.sacrificedPower]).
 */
val JaradGolgariLichLord = card("Jarad, Golgari Lich Lord") {
    manaCost = "{B}{B}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Zombie Elf"
    power = 2
    toughness = 2
    oracleText = "Jarad gets +1/+1 for each creature card in your graveyard.\n" +
        "{1}{B}{G}, Sacrifice another creature: Each opponent loses life equal to the sacrificed creature's power.\n" +
        "Sacrifice a Swamp and a Forest: Return this card from your graveyard to your hand."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.creatureCardsInYourGraveyard(),
            toughnessBonus = DynamicAmounts.creatureCardsInYourGraveyard()
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}{G}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.LoseLife(DynamicAmounts.sacrificedPower(), EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.SWAMP)),
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.FOREST)),
        )
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "45"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6a2f10c-721c-40c7-8fe1-b4877a40fe96.jpg?1783940402"
        ruling("2012-10-01", "Jarad's first ability applies only when Jarad is on the battlefield.")
        ruling("2012-10-01", "The life loss is based on the power of the sacrificed creature as it last existed on the battlefield.")
        ruling("2012-10-01", "To activate Jarad's last ability, you must sacrifice two lands. It doesn't matter what other land types the two lands have, as long as one is a Swamp and one is a Forest. Even if one of the lands you sacrifice is both a Swamp and a Forest, you have to sacrifice another land that is a Swamp or a Forest.")
        ruling("2012-10-01", "You can sacrifice any land with the land type Swamp or Forest, not just ones named Swamp or Forest. For example, you could sacrifice two Overgrown Tombs.")
    }
}
