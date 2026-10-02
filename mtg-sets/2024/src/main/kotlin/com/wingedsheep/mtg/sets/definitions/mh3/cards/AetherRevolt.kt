package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Aether Revolt
 * {2}{R}{R}
 * Enchantment
 * Revolt — As long as a permanent left the battlefield under your control this turn, if a source
 * you control would deal noncombat damage to an opponent or a permanent an opponent controls, it
 * deals that much damage plus 2 instead.
 * Whenever you get one or more {E}, this enchantment deals that much damage to any target.
 *
 * The revolt rider is a plain additive damage replacement gated by the revolt condition through
 * [ModifyDamageAmount]'s `restrictions` (read against this enchantment's controller, like Far
 * Fortune's max-speed rider); the extra damage is dealt by the original source.
 *
 * The energy trigger names the *player* recipient (`Triggers.you.getsCounters`), so energy given to
 * you by anyone's effect fires it, and one grant of several {E} fires it once for the whole amount.
 */
val AetherRevolt = card("Aether Revolt") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Revolt — As long as a permanent left the battlefield under your control this turn, " +
        "if a source you control would deal noncombat damage to an opponent or a permanent an opponent " +
        "controls, it deals that much damage plus 2 instead.\n" +
        "Whenever you get one or more {E}, this enchantment deals that much damage to any target."

    replacementEffect(
        ModifyDamageAmount(
            modifier = 2,
            restrictions = listOf(Conditions.YouHadPermanentLeaveBattlefieldThisTurn),
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.youControl(),
                recipient = Recipient.OpponentOrPermanentTheyControl,
                damageType = DamageType.NonCombat,
            ),
        )
    )

    triggeredAbility {
        trigger = Triggers.you.getsCounters(CounterType.ENERGY)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.triggerCountersPlaced(), t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Filipe Pagliuso"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8732e62-cdee-4d32-82a8-8a71a04be7a1.jpg?1783911274"
        ruling("2024-06-07", "Once a permanent you control leaves the battlefield, Aether Revolt's revolt ability will apply to noncombat damage dealt by sources you control until the turn ends or Aether Revolt leaves the battlefield, whichever happens first. It doesn't matter what happens to the card or token that left the battlefield afterward.")
        ruling("2024-06-07", "The additional damage is dealt by the original source of the damage, not by Aether Revolt.")
        ruling("2024-06-07", "If you get multiple {E} at once, Aether Revolt's last ability will trigger only once. When the triggered ability resolves, it will deal damage to the target equal to the amount of {E} you gained.")
        ruling("2024-06-07", "If an effect instructs you to get one or more {E} and then allows you to spend {E}, Aether Revolt's last ability will see the amount of {E} you got. It doesn't matter how much {E} you spend after that.")
    }
}
