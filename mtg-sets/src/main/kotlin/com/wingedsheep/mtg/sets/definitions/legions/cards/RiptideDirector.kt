package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Riptide Director
 * {2}{U}{U}
 * Creature — Human Wizard
 * 2/3
 * {2}{U}{U}, {T}: Draw a card for each Wizard you control.
 */
val RiptideDirector = card("Riptide Director") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "{2}{U}{U}, {T}: Draw a card for each Wizard you control."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}{U}"),
            Costs.Tap
        )
        effect = DrawCardsEffect(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withSubtype(Subtype.WIZARD)).count(),
            target = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Scott M. Fischer"
        flavorText = "\"Those who lead others to wisdom become the wisest themselves.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28d07de3-b176-4ac7-aaa7-497c06c08b55.jpg?1562903326"
    }
}
