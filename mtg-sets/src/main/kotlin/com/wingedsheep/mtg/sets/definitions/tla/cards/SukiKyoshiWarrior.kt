package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Suki, Kyoshi Warrior
 * {2}{G/W}{G/W}
 * Legendary Creature — Human Warrior Ally
 * Power: *  Toughness: 4
 *
 * Suki's power is equal to the number of creatures you control.
 * Whenever Suki attacks, create a 1/1 white Ally creature token that's tapped and attacking.
 *
 * Power is a characteristic-defining ability (counts every creature you control, including
 * Suki herself); toughness is the printed fixed value 4.
 */
val SukiKyoshiWarrior = card("Suki, Kyoshi Warrior") {
    manaCost = "{2}{G/W}{G/W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Warrior Ally"
    dynamicPower(
        DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Creature,
        ),
    )
    toughness = 4
    oracleText = "Suki's power is equal to the number of creatures you control.\n" +
        "Whenever Suki attacks, create a 1/1 white Ally creature token that's tapped and attacking."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(1),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Ally"),
            tapped = true,
            attacking = true,
        )
        description = "Whenever Suki attacks, create a 1/1 white Ally creature token that's tapped and attacking."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Yuhong Ding"
        flavorText = "\"Hoping for another dance lesson?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e311480-04f8-4b8f-938e-1cc6d06b9902.jpg?1764121794"
    }
}
