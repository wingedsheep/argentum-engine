package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantPayLifeForCosts
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Karn's Sylex
 * {3}
 * Legendary Artifact
 * Karn's Sylex enters tapped.
 * Players can't pay life to cast spells or to activate abilities that aren't mana abilities.
 * {X}, {T}, Exile Karn's Sylex: Destroy each nonland permanent with mana value X or less.
 * Activate only as a sorcery.
 *
 * The prohibition reaches only costs: life paid while something resolves (ward, "unless … pays N
 * life") and a mana ability's life cost stay payable (printed rulings).
 */
val KarnsSylex = card("Karn's Sylex") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Karn's Sylex enters tapped.\n" +
        "Players can't pay life to cast spells or to activate abilities that aren't mana abilities.\n" +
        "{X}, {T}, Exile Karn's Sylex: Destroy each nonland permanent with mana value X or less. " +
        "Activate only as a sorcery."
    replacementEffect(EntersTapped())
    staticAbility {
        ability = PlayersCantPayLifeForCosts(excludeManaAbilities = true)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap, Costs.ExileSelf)
        effect = Effects.DestroyAll(GameObjectFilter.NonlandPermanent.manaValueAtMostX())
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "234"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/673a8676-0a93-4ca3-a786-63f138027e8a.jpg?1783921268"
    }
}
