package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS

/**
 * G'raha Tia, Scion Reborn (FIC #3)
 * {W}{U}{B}
 * Legendary Creature — Cat Wizard
 * 2/3
 * Lifelink
 * Throw Wide the Gates — Whenever you cast a noncreature spell, you may pay X life, where X is that
 * spell's mana value. If you do, create a 1/1 colorless Hero creature token and put X +1/+1
 * counters on it. Do this only once each turn.
 *
 * X is the triggering spell's mana value captured as it was cast (X included, cost changes not —
 * the rulings), so the trigger still resolves fully if the spell is countered first. The life
 * payment is the consent gate of [Effects.MayPay]; "Do this only once each turn" is
 * `effectOncePerTurn` (CR 603.2h), keyed to the payment — declining leaves the ability live for the
 * next noncreature spell, and once you've paid, it stops triggering for the rest of the turn.
 */
val GrahaTiaScionReborn = card("G'raha Tia, Scion Reborn") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Cat Wizard"
    power = 2
    toughness = 3
    oracleText = "Lifelink\n" +
        "Throw Wide the Gates — Whenever you cast a noncreature spell, you may pay X life, where X is " +
        "that spell's mana value. If you do, create a 1/1 colorless Hero creature token and put X +1/+1 " +
        "counters on it. Do this only once each turn."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effectOncePerTurn = true
        effect = Effects.MayPay(
            cost = Effects.PayDynamicLife(DynamicAmounts.triggeringSpellManaValue()),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Hero"),
                imageUri = "https://cards.scryfall.io/normal/front/d/0/d0657ce1-bf75-4007-ac1b-0623eb263357.jpg?1783906138",
            ) then Effects.AddCountersToCollection(
                CREATED_TOKENS,
                CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.triggeringSpellManaValue(),
            )
        )
        description = "Throw Wide the Gates — Whenever you cast a noncreature spell, you may pay X life, " +
            "where X is that spell's mana value. If you do, create a 1/1 colorless Hero creature token and " +
            "put X +1/+1 counters on it. Do this only once each turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "YASUNARI HIRASAKA"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f90eb8ef-ad70-4d6c-9958-e6153f8599bc.jpg?1783906373"

        ruling("2025-06-06", "Alternative costs, additional costs, and cost reductions don't change a spell's mana value. Its mana value is still based on its mana cost.")
        ruling("2025-06-06", "For spells on the stack with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
        ruling("2025-06-06", "G'raha Tia's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
