package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phoenix Chick
 * {R}
 * Creature — Phoenix
 * 1/1
 * Flying, haste
 * This creature can't block.
 * Whenever you attack with three or more creatures, you may pay {R}{R}. If you do, return this
 * card from your graveyard to the battlefield tapped and attacking with a +1/+1 counter on it.
 */
val PhoenixChick = card("Phoenix Chick") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 1
    toughness = 1
    oracleText = "Flying, haste\nThis creature can't block.\nWhenever you attack with three or more creatures, you may pay {R}{R}. If you do, return this card from your graveyard to the battlefield tapped and attacking with a +1/+1 counter on it."

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 3)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{R}{R}"),
            then = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD,
                placement = ZonePlacement.TappedAndAttacking,
                addCounterType = CounterType.PLUS_ONE_PLUS_ONE
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76bdc912-61da-428d-b0d7-3a38676a402a.jpg?1783921312"
        ruling("2022-09-09", "As Phoenix Chick returns to the battlefield because of its triggered ability, you choose which opponent or opposing planeswalker it's attacking. It doesn't have to attack the same opponent or opposing planeswalker as your other attacking creatures.")
        ruling("2022-09-09", "If Phoenix Chick enters the battlefield attacking, it wasn't declared as an attacking creature that turn. Abilities that trigger when a creature attacks won't trigger.")
    }
}
