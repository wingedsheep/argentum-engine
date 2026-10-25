package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Mina and Denn, Wildborn
 * {2}{R}{G}
 * Legendary Creature — Elf Ally
 * 4/4
 * You may play an additional land on each of your turns.
 * {R}{G}, Return a land you control to its owner's hand: Target creature gains trample until end of turn.
 *
 * The extra land drop is the static [GrantAdditionalLandDrop] (Exploration). Returning the land is
 * part of the activation cost (`Costs.ReturnToHand`, the Quirion Ranger shape), so it can't be
 * interrupted once announced (ruling).
 */
val MinaAndDennWildborn = card("Mina and Denn, Wildborn") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Elf Ally"
    oracleText = "You may play an additional land on each of your turns.\n" +
        "{R}{G}, Return a land you control to its owner's hand: Target creature gains trample until end of turn."
    power = 4
    toughness = 4

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}{G}"), Costs.ReturnToHand(GameObjectFilter.Land))
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "{R}{G}, Return a land you control to its owner's hand: Target creature gains " +
            "trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Izzy"
        flavorText = "Greenweaver Mina and her twin brother Denn lead the last of the Mul Daya into an uncertain future."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d27ff523-0ff3-480d-ae95-ff5e04483e40.jpg?1783937896"
        ruling(
            "2016-01-22",
            "Once you announce that you're activating the last ability, it's too late for anyone to " +
                "interrupt you by trying to remove the land you returned."
        )
    }
}
