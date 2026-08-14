package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slith Firewalker — Mirrodin #107
 * {R}{R} · Creature — Slith · 1/1
 *
 * Haste
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * The head of the Slith cycle's red member. Haste plus the growth trigger is the whole point:
 * it can attack the turn it lands and start compounding immediately.
 *
 * The trigger is [Triggers.DealsCombatDamageToPlayer] — *combat* damage only, so a burn-style
 * ping or a damage-redirection effect never grows it — and the counter goes on the Slith itself
 * ([EffectTarget.Self]), not on a chosen creature.
 */
val SlithFirewalker = card("Slith Firewalker") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Slith"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Justin Sweet"
        flavorText = "The slith incubate in the Great Furnace's heat, emerging on Mirrodin's " +
            "surface only when the four suns have aligned overhead."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73ff7383-71da-46ae-849f-349c40815a29.jpg?1783944537"
    }
}
