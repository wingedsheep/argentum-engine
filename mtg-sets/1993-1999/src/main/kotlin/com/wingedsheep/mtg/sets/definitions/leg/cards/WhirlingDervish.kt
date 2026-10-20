package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.SourceDealtDamageToPlayerThisTurn
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Whirling Dervish — Legends #211
 * {G}{G} · Creature — Human Monk · 1/1 · Uncommon
 *
 * Protection from black
 * At the beginning of each end step, if this creature dealt damage to an opponent this turn,
 * put a +1/+1 counter on it.
 *
 * "An opponent" is existential over every opponent: `CountPlayersWith` rebinds `Player.You` to
 * each opponent in turn, so `SourceDealtDamageToPlayerThisTurn(Player.You)` asks whether the
 * Dervish dealt damage to *that* opponent. The source's damage memory covers combat and noncombat
 * damage and is wiped on a zone change, so a Dervish that left and returned has dealt none.
 */
val WhirlingDervish = card("Whirling Dervish") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 1
    oracleText = "Protection from black\nAt the beginning of each end step, if this creature dealt " +
        "damage to an opponent this turn, put a +1/+1 counter on it."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countPlayersWith(Player.EachOpponent, SourceDealtDamageToPlayerThisTurn(Player.You)),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eba294e7-7097-4bc3-b396-72e85dd4f441.jpg?1783948043"
    }
}
