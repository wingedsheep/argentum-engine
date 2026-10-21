package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Samut, Vizier of Naktamun — March of the Machine: The Aftermath #45
 * {1}{R}{G} · Legendary Creature — Human Warrior Cleric · 2/3
 *
 * First strike, vigilance, haste
 * Whenever a creature you control deals combat damage to a player, if that creature entered this
 * turn, draw a card.
 *
 * A per-creature observer trigger (not the "one or more" batch form) with the CR 603.4
 * intervening-if on the *damaging* creature: checked as the trigger fires and again on
 * resolution, where a creature that has since left reads its last-known "entered this turn".
 */
val SamutVizierOfNaktamun = card("Samut, Vizier of Naktamun") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warrior Cleric"
    power = 2
    toughness = 3
    oracleText = "First strike, vigilance, haste\n" +
        "Whenever a creature you control deals combat damage to a player, if that creature entered this turn, draw a card."

    keywords(Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.EntityMatches(
            EffectTarget.TriggeringEntity,
            GameObjectFilter.Any.enteredThisTurn()
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "45"
        artist = "Jarel Threat"
        flavorText = "\"Try to keep up, Djeru. We have lots of work to do.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0f3fdb0-47ad-4df6-a95c-a2c81aaf7af5.jpg?1783916508"
    }
}
