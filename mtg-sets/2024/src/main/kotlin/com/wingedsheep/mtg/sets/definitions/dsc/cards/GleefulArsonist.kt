package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gleeful Arsonist
 * {2}{R}
 * Creature — Human Wizard
 * 1/2
 * Whenever an opponent casts a noncreature spell, this creature deals damage equal to its power to
 * that player.
 * Undying
 *
 * "That player" is the caster ([Player.TriggeringPlayer]); the amount is read as the trigger
 * resolves, from the Arsonist's last-known power if it has left the battlefield.
 */
val GleefulArsonist = card("Gleeful Arsonist") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Whenever an opponent casts a noncreature spell, this creature deals damage equal to " +
        "its power to that player.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the " +
        "battlefield under its owner's control with a +1/+1 counter on it.)"

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.sourcePower(),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    keywords(Keyword.UNDYING)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Nereida"
        flavorText = "\"Let the world burn!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbb2956c-ef76-4e80-81d4-7f1ce6841d37.jpg?1783909657"
        ruling("2024-09-20", "Gleeful Arsonist's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2024-09-20", "If Gleeful Arsonist is no longer on the battlefield when its first ability resolves, use its power as it last existed on the battlefield to determine how much damage it deals.")
    }
}
