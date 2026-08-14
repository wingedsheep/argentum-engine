package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slith Ascendant — Mirrodin #23
 * {1}{W}{W} · Creature — Slith · 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * The white member of the Slith cycle (see [SlithBloodletter], [SlithFirewalker],
 * [SlithPredator]) — same growth trigger, with evasion instead of a mana rider. Flying is
 * what makes the compounding reliable: the +1/+1 counter only lands on a *connected* attack
 * ([Triggers.DealsCombatDamageToPlayer] is combat damage to a player, so a blocked Slith or a
 * ping from an activated ability grows nothing), and a 1/1 flier connects far more often than
 * a ground body of the same size.
 */
val SlithAscendant = card("Slith Ascendant") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Slith"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Justin Sweet"
        flavorText = "Instinctively drawn to the light of its \"mother-sun,\" each slith follows " +
            "that sun's path around Mirrodin."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0286b42a-295b-467c-a1d8-0b31774b7ac5.jpg?1783944559"
    }
}
