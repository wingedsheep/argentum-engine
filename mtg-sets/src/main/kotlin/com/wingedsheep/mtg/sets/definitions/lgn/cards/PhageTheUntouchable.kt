package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phage the Untouchable
 * {3}{B}{B}{B}{B}
 * Legendary Creature — Avatar Minion
 * 4/4
 * When Phage enters, if you didn't cast it from your hand, you lose the game.
 * Whenever Phage deals combat damage to a creature, destroy that creature. It can't be regenerated.
 * Whenever Phage deals combat damage to a player, that player loses the game.
 */
val PhageTheUntouchable = card("Phage the Untouchable") {
    manaCost = "{3}{B}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Avatar Minion"
    power = 4
    toughness = 4
    oracleText = "When Phage the Untouchable enters, if you didn't cast it from your hand, you lose the game.\nWhenever Phage the Untouchable deals combat damage to a creature, destroy that creature. It can't be regenerated.\nWhenever Phage the Untouchable deals combat damage to a player, that player loses the game."

    // ETB: if you didn't cast it from your hand, you lose the game
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.Not(Conditions.WasCastFromHand)
        effect = Effects.LoseGame(
            target = EffectTarget.Controller,
            message = "Phage the Untouchable was not cast from hand"
        )
    }

    // Combat damage to creature: destroy, can't be regenerated
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToCreature
        effect = CantBeRegeneratedEffect(EffectTarget.TriggeringEntity) then
                Effects.Move(EffectTarget.TriggeringEntity, Zone.GRAVEYARD, byDestruction = true)
    }

    // Combat damage to player: that player loses the game
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.LoseGame(
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            message = "Phage the Untouchable dealt combat damage"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a410b933-99d0-4383-b54b-4839a76eb6fe.jpg?1562928176"
        ruling("2004-10-04", "It is only considered 'cast from your hand' if you cast it as a spell from your hand. Putting it onto the battlefield from your hand using a spell or ability will cause you to lose the game.")
        ruling("2013-07-01", "In a Commander game where this card is your commander, casting it from the Command zone does not count as casting it from your hand.")
    }
}
