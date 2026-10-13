package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vodalian Mindsinger
 * {1}{U}{U}
 * Creature — Merfolk Wizard
 * 2/2
 *
 * Kicker {1}{R} and/or {1}{G}
 * This creature enters with two +1/+1 counters on it for each time it was kicked.
 * When this creature enters, gain control of target creature with power less than this creature's
 * power for as long as you control this creature.
 *
 * - "Kicker {1}{R} and/or {1}{G}" is two kicker abilities (CR 702.33b), declared in printed order;
 *   "each time it was kicked" is one per linked kicker slot paid (0, 1 or 2) — the Urborg Lhurgoyf
 *   shape, two counters per slot, as an as-enters replacement so the counters are there before the
 *   enters trigger targets.
 * - The steal is Rangers of Ithilien's shape: strict `powerLessThanEntity(Self)` against the
 *   Mindsinger's projected power (rechecked on resolution, ruling), for
 *   `Duration.WhileYouControlSource`. Later power changes don't end the effect (ruling).
 */
val VodalianMindsinger = card("Vodalian Mindsinger") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "URG"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Kicker {1}{R} and/or {1}{G} (You may pay an additional {1}{R} and/or {1}{G} as you cast this spell.)\n" +
        "This creature enters with two +1/+1 counters on it for each time it was kicked.\n" +
        "When this creature enters, gain control of target creature with power less than this creature's " +
        "power for as long as you control this creature."

    keywordAbility(KeywordAbility.kicker("{1}{R}"))
    keywordAbility(KeywordAbility.kicker("{1}{G}"))

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.conditional(Conditions.WasKickedWithFirstKicker, 2, 0) +
                DynamicAmounts.conditional(Conditions.WasKickedWithSecondKicker, 2, 0)
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.powerLessThanEntity(EffectTarget.Self))
        )
        effect = Effects.GainControl(creature, Duration.WhileYouControlSource("Vodalian Mindsinger"))
        description = "When this creature enters, gain control of target creature with power less than this " +
            "creature's power for as long as you control this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/211a9db2-8b5f-419d-8032-ac2a571dbb53.jpg?1783921339"
        ruling("2022-09-09", "If the target creature's power is greater than or equal to Vodalian Mindsinger's power when the triggered ability resolves, it will be an illegal target for the ability, and you will not gain control of it. Once the ability resolves, however, changing the power of either creature will not end the control-changing effect.")
        ruling("2022-09-09", "You may kick Vodalian Mindsinger only once for its {1}{R} cost and only once for its {1}{G} cost. You can kick it twice by paying {2}{R}{G}, but you can't kick it twice by paying {2}{R}{R} or {2}{G}{G}. You can't kick it more than twice.")
    }
}
