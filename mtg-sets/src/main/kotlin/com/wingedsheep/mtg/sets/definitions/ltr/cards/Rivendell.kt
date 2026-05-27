package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rivendell
 * Legendary Land
 *
 * Rivendell enters tapped unless you control a legendary creature.
 * {T}: Add {U}.
 * {1}{U}, {T}: Scry 2. Activate only if you control a legendary creature.
 */
val Rivendell = card("Rivendell") {
    typeLine = "Legendary Land"
    colorIdentity = "U"
    oracleText = "Rivendell enters tapped unless you control a legendary creature.\n{T}: Add {U}.\n" +
        "{1}{U}, {T}: Scry 2. Activate only if you control a legendary creature."

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.legendary()
            )
        )
    )

    // {T}: Add {U}.
    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}{U}, {T}: Scry 2. Activate only if you control a legendary creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        effect = EffectPatterns.scry(2)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Exists(
                    player = Player.You,
                    zone = Zone.BATTLEFIELD,
                    filter = GameObjectFilter.Creature.legendary()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Jonas De Ro"
        flavorText = "\"Were I to go where my heart dwells, I would now be wandering in the fair valley of Rivendell.\"\n—Aragorn"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bacd500c-1389-4314-a53e-0ad510d6fb79.jpg?1686970394"
    }
}
