package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Daxos of Meletis
 * {1}{W}{U}
 * Legendary Creature — Human Soldier
 * 2/2
 * Daxos can't be blocked by creatures with power 3 or greater.
 * Whenever Daxos deals combat damage to a player, exile the top card of that player's library.
 * You gain life equal to that card's mana value. Until end of turn, you may cast that card and
 * you may spend mana as though it were mana of any color to cast that spell.
 *
 * Evasion is a [CantBeBlockedBy] static keyed on blocker power. The trigger is Robber of the
 * Rich's impulse shape aimed at the damaged player ([Player.TriggeringPlayer]): gather → exile
 * their top card, gain life equal to the exiled card's mana value (Triple Triad's
 * `manaValueOf(collection)`), then an end-of-turn, cast-only (`nonLandOnly`) grant with
 * any-color mana spending.
 */
val DaxosOfMeletis = card("Daxos of Meletis") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Daxos can't be blocked by creatures with power 3 or greater.\n" +
        "Whenever Daxos deals combat damage to a player, exile the top card of that player's " +
        "library. You gain life equal to that card's mana value. Until end of turn, you may cast " +
        "that card and you may spend mana as though it were mana of any color to cast that spell."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(3))
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1, player = Player.TriggeringPlayer))
            exile(exiled, Player.TriggeringPlayer)
            run(Effects.GainLife(DynamicAmounts.manaValueOf(exiled)))
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.EndOfTurn,
                nonLandOnly = true,
                withAnyManaColor = true
            ))
        }
        description = "Whenever Daxos deals combat damage to a player, exile the top card of that " +
            "player's library. You gain life equal to that card's mana value. Until end of turn, " +
            "you may cast that card and you may spend mana as though it were mana of any color to " +
            "cast that spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Karla Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bb68644-e99d-4ecb-a804-8d4ad1c3325d.jpg?1783939731"
    }
}
