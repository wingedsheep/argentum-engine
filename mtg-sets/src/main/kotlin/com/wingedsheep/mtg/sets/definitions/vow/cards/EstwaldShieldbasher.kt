package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Estwald Shieldbasher
 * {3}{W}
 * Creature — Human Soldier
 * 4/2
 *
 * Whenever this creature attacks, you may pay {1}. If you do, it gains indestructible until end of
 * turn.
 *
 * The attack trigger is a flat "you may pay {1}. If you do" gate ([MayPayManaEffect]); paying
 * grants indestructible to the attacker itself for the turn.
 */
val EstwaldShieldbasher = card("Estwald Shieldbasher") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 4
    toughness = 2
    oracleText = "Whenever this creature attacks, you may pay {1}. If you do, it gains " +
        "indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may pay {1}. If you do, it gains " +
            "indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Wayne Reynolds"
        flavorText = "The door to her home survived the fires of the Malignus. Now it's her " +
            "greatest weapon."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61c7d889-8cc0-4e80-b6d5-d41961820224.jpg?1782703190"
    }
}
