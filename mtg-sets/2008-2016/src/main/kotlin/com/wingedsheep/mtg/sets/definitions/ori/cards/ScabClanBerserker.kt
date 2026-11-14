package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scab-Clan Berserker
 * {1}{R}{R}
 * Creature — Human Berserker
 * 2/2
 *
 * Haste
 * Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)
 * Whenever an opponent casts a noncreature spell, if this creature is renowned, this creature deals 2 damage to that player.
 *
 * "If this creature is renowned" is an intervening-if (checked on trigger and again on resolution);
 * "that player" is the caster, bound by the opponent-casts trigger as [Player.TriggeringPlayer].
 */
val ScabClanBerserker = card("Scab-Clan Berserker") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    oracleText = "Haste\n" +
        "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n" +
        "Whenever an opponent casts a noncreature spell, if this creature is renowned, this creature deals 2 damage to that player."
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)
    keywordAbility(KeywordAbility.renown(1))

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        interveningIf = Conditions.SourceIsRenowned
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38d600b0-8649-424a-acad-879f3067b55e.jpg?1783938326"
    }
}
