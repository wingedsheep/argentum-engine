package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hanna, Ship's Navigator
 * {1}{W}{U}
 * Legendary Creature — Human Artificer
 * 1/2
 * {1}{W}{U}, {T}: Return target artifact or enchantment card from your graveyard to your hand.
 */
val HannaShipsNavigator = card("Hanna, Ship's Navigator") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "{1}{W}{U}, {T}: Return target artifact or enchantment card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}{U}"), Costs.Tap)
        val t = target(
            "target",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.ArtifactOrEnchantment.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.ReturnToHand(t)
        description = "{1}{W}{U}, {T}: Return target artifact or enchantment card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83a4e48d-6452-4245-bdad-63fe3263550e.jpg?1562921669"
    }
}
