package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming
import com.wingedsheep.sdk.dsl.DynamicAmounts

val GuardianAngel = card("Guardian Angel") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent the next X damage that would be dealt to any target this turn. Until end of turn, you may pay {1} any time you could cast an instant. If you do, prevent the next 1 damage that would be dealt to that permanent or player this turn."

    spell {
        val recipient = target(Targets.Any)
        effect = Effects.PreventDamage(target = recipient, amount = DynamicAmounts.xValue()) then
            Effects.GrantPlayerAction(
                cost = Costs.pay.Mana("{1}"), effect = Effects.PreventDamage(target = recipient, amount = DynamicAmounts.fixed(1)),
                timing = PlayerActionTiming.Instant,
                actionDescription = "Guardian Angel: Pay {1} to prevent the next 1 damage",
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f84d676-5327-454c-a033-b4498a9d28e2.jpg?1783948714"
    }
}
