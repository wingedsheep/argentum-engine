package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shu Yun, the Silent Tempest
 * {2}{U}
 * Legendary Creature — Human Monk
 * 3/2
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Whenever you cast a noncreature spell, you may pay {R/W}{R/W}. If you do, target creature gains double strike until end of turn.
 *
 * The target is chosen as the trigger goes on the stack; the {R/W}{R/W} payment is offered on
 * resolution, and double strike is granted only if it is paid. Prowess is `prowess()` so its
 * +1/+1 trigger exists, not only the keyword.
 */
val ShuYunTheSilentTempest = card("Shu Yun, the Silent Tempest") {
    manaCost = "{2}{U}"
    colorIdentity = "RUW"
    typeLine = "Legendary Creature — Human Monk"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Whenever you cast a noncreature spell, you may pay {R/W}{R/W}. If you do, target creature gains double strike until end of turn."
    power = 3
    toughness = 2

    prowess()

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        val creature = target(TargetFilter.Creature)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{R/W}{R/W}"),
            then = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd79e83a-a34b-472c-9317-12af4ed2ca28.jpg"
    }
}
