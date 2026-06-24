package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Kitsune, Dragon's Daughter
 * {4}{U}{U}
 * Legendary Creature — Fox Warlock Avatar
 * 6/6
 *
 * Vigilance
 * Whenever Kitsune enters or deals combat damage to a player, you may exchange
 * control of two other target creatures controlled by different players.
 *
 * In the engine's two-player game, "controlled by different players" is exactly
 * one creature you control and one an opponent controls.
 */
val KitsuneDragonsDaughter = card("Kitsune, Dragon's Daughter") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Fox Warlock Avatar"
    oracleText = "Vigilance\nWhenever Kitsune enters or deals combat damage to a player, you may exchange control of two other target creatures controlled by different players."
    power = 6
    toughness = 6

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val yours = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true))
        )
        val theirs = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls)
        )
        effect = MayEffect(Effects.ExchangeControl(yours, theirs))
        description = "When Kitsune enters, you may exchange control of two other target creatures controlled by different players."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val yours = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true))
        )
        val theirs = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls)
        )
        effect = MayEffect(Effects.ExchangeControl(yours, theirs))
        description = "Whenever Kitsune deals combat damage to a player, you may exchange control of two other target creatures controlled by different players."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Robin Har"
        flavorText = "\"You hunger for retribution, I know. And so I come to you with an offering . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a87a9257-4535-4286-8b59-a842ac45d05e.jpg?1769005699"
    }
}
