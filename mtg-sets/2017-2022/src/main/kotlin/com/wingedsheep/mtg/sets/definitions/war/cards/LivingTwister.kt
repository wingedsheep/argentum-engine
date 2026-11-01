package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Living Twister
 * {R}{R}{G}
 * Creature — Elemental
 * 2/5
 * {1}{R}, Discard a land card: This creature deals 2 damage to any target.
 * {G}: Return a tapped land you control to its owner's hand.
 *
 * "Return a tapped land you control" doesn't target: the land is chosen as the ability resolves,
 * so it gathers, chooses and moves (the Gruul Turf shape) rather than binding a target.
 */
val LivingTwister = card("Living Twister") {
    manaCost = "{R}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 5
    oracleText = "{1}{R}, Discard a land card: This creature deals 2 damage to any target.\n" +
        "{G}: Return a tapped land you control to its owner's hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Discard(Filters.Land))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.Pipeline {
            val lands = gather(
                CardSource.BattlefieldMatching(filter = GameObjectFilter.Land.tapped(), player = Player.You)
            )
            val returned = chooseExactly(
                1,
                from = lands,
                prompt = "Return a tapped land you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "James Paick"
        flavorText = "\"The ancient places of Ravnica awaken, stirred by the hoofbeats of Ilharg!\"\n—Nikya of the Old Ways"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8d92676-a994-4031-834c-6cf7c43bd170.jpg?1783933395"
    }
}
