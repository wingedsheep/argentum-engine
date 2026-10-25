package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Krydle of Baldur's Gate
 * {U}{B}
 * Legendary Creature — Human Elf Rogue
 * 1/3
 * Whenever Krydle deals combat damage to a player, that player loses 1 life and mills a card,
 * then you gain 1 life and scry 1.
 * Whenever you attack, you may pay {2}. If you do, target creature can't be blocked this turn.
 */
val KrydleOfBaldursGate = card("Krydle of Baldur's Gate") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Elf Rogue"
    power = 1
    toughness = 3
    oracleText = "Whenever Krydle deals combat damage to a player, that player loses 1 life and mills a card, " +
        "then you gain 1 life and scry 1.\n" +
        "Whenever you attack, you may pay {2}. If you do, target creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val damagedPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        effect = Effects.LoseLife(1, damagedPlayer) then
            Patterns.Library.mill(1, damagedPlayer) then
            Effects.GainLife(1) then
            Effects.Scry(1)
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75f4d622-2d50-4e6e-87a5-5a1cc65d6919.jpg?1783926446"
    }
}
