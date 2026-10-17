package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nashi, Moon Sage's Scion — Kamigawa: Neon Dynasty #114 (canonical printing)
 * {1}{B}{B} · Legendary Creature — Rat Ninja · 3/2
 *
 * Ninjutsu {3}{B}
 * Whenever Nashi deals combat damage to a player, exile the top card of each player's library.
 * Until end of turn, you may play one of those cards. If you cast a spell this way, pay life
 * equal to its mana value rather than paying its mana cost.
 *
 * "One of those cards" is a `maxPlays = 1` grant over the whole pile; the life-for-mana-value
 * alternative cost composes the waiver and the life payment the same way Inside Information
 * does. Lands are played normally and take neither cast-only component.
 */
val NashiMoonSagesScion = card("Nashi, Moon Sage's Scion") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Rat Ninja"
    power = 3
    toughness = 2
    oracleText = "Ninjutsu {3}{B} ({3}{B}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever Nashi deals combat damage to a player, exile the top card of each player's " +
        "library. Until end of turn, you may play one of those cards. If you cast a spell this way, " +
        "pay life equal to its mana value rather than paying its mana cost."

    ninjutsu("{3}{B}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val nashiExiled = gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
            exile(nashiExiled)
            run(Effects.GrantMayPlayFromExile(from = nashiExiled, maxPlays = 1))
            run(Effects.GrantPlayWithoutPayingCost(nashiExiled))
            run(Effects.GrantPlayWithAdditionalCost(
                from = nashiExiled,
                additionalCost = Costs.additional.PayLifeEqualToManaValueOfSpell,
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2893b070-a610-4273-beb2-7609a2da0499.jpg?1783923879"
    }
}
