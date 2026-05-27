package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rith, the Awakener
 * {3}{R}{G}{W}
 * Legendary Creature — Dragon
 * 6/6
 * Flying
 * Whenever Rith deals combat damage to a player, you may pay {2}{G}. If you do, choose a color,
 * then create a 1/1 green Saproling creature token for each permanent of that color.
 *
 * One of the five Coalition Dragons. "Each permanent of that color" counts every permanent on
 * the battlefield (any controller), so it's an `AggregateBattlefield(Player.Each)` over the
 * chosen-color filter — `withChosenColor()` reads the color stored by [Effects.ChooseColorThen].
 */
val RithTheAwakener = card("Rith, the Awakener") {
    manaCost = "{3}{R}{G}{W}"
    colorIdentity = "GRW"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\nWhenever Rith deals combat damage to a player, you may pay {2}{G}. If you do, " +
        "choose a color, then create a 1/1 green Saproling creature token for each permanent of that color."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{G}"),
            effect = Effects.ChooseColorThen(
                then = CreateTokenEffect(
                    count = DynamicAmount.AggregateBattlefield(
                        player = Player.Each,
                        filter = GameObjectFilter.Permanent.withChosenColor()
                    ),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Saproling"),
                    imageUri = "https://cards.scryfall.io/normal/front/5/3/5371de1b-db33-4db4-a518-e35c71aa72b7.jpg?1562702067"
                ),
                prompt = "Choose a color"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Carl Critchlow"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c30be387-280d-49bd-a3d1-c1636ee931ce.jpg?1562934165"
    }
}
