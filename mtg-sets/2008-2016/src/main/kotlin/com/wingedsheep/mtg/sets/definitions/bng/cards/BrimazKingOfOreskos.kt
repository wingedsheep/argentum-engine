package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private fun catSoldier(attacking: Boolean) = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Cat", "Soldier"),
    keywords = setOf(Keyword.VIGILANCE),
    attacking = attacking,
    attackDefender = if (attacking) EnteringAttackDefender.CONTROLLER_CHOOSES else EnteringAttackDefender.JOIN_SOURCE,
)

/**
 * Brimaz, King of Oreskos — Born of the Gods #5
 *
 * The attack token chooses its own defender (CR 508.4). The block token is created, then put into
 * the block as it enters (CR 509.4): `BecomeBlocking(entering = true)` over the created tokens, so
 * it never "blocked" — no blocks triggers for it — while the attacker's becomes-blocked triggers
 * still see it. If that creature is no longer attacking, the token just enters (CR 509.4a).
 */
val BrimazKingOfOreskos = card("Brimaz, King of Oreskos") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Cat Soldier"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "Whenever Brimaz attacks, create a 1/1 white Cat Soldier creature token with vigilance that's attacking.\n" +
        "Whenever Brimaz blocks a creature, create a 1/1 white Cat Soldier creature token with vigilance that's blocking that creature."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = catSoldier(attacking = true)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature)
        effect = catSoldier(attacking = false) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.BecomeBlocking(EffectTarget.IterationEntity, EffectTarget.TriggeringEntity, entering = true),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f54eb705-6326-4bac-bf0e-68d42db7a270.jpg?1783939586"
        ruling("2014-02-01", "For the first triggered ability, you declare which player, planeswalker, or battle the token is attacking as you put it onto the battlefield. It doesn't have to be the same player or planeswalker Brimaz is attacking.")
        ruling("2014-02-01", "If Brimaz somehow blocks two or more creatures (note it can't naturally do this), its last ability will trigger that many times.")
        ruling("2014-02-01", "Although the tokens enter the battlefield attacking or blocking, they were never declared as attacking or blocking creatures. Abilities that trigger whenever a creature attacks or blocks won't trigger.")
    }
}
