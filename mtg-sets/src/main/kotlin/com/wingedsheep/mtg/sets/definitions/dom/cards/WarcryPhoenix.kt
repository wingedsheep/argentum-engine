package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.YouAttackEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Warcry Phoenix
 * {3}{R}
 * Creature — Phoenix
 * 2/2
 * Flying, haste
 * Whenever you attack with three or more creatures, you may pay {2}{R}. If you do,
 * return Warcry Phoenix from your graveyard to the battlefield tapped and attacking.
 */
val WarcryPhoenix = card("Warcry Phoenix") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\nWhenever you attack with three or more creatures, you may pay {2}{R}. If you do, return Warcry Phoenix from your graveyard to the battlefield tapped and attacking."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = TriggerSpec(YouAttackEvent(minAttackers = 3), TriggerBinding.ANY)
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{R}"),
            effect = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD,
                placement = ZonePlacement.TappedAndAttacking
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Daarken"
        flavorText = "\"War begins with one red ember.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff561f01-8dbc-4988-be00-592d1e417396.jpg?1562746405"
    }
}
