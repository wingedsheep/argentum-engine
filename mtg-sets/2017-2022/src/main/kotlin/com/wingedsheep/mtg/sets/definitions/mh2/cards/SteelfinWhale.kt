package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Steelfin Whale — Modern Horizons 2 #65
 * {5}{U} · Creature — Whale · 3 / 4
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Whenever an artifact you control enters, untap this creature.
 *
 * Pure composition. [KeywordAbility.Affinity] for [CardType.ARTIFACT] is engine-live vocabulary —
 * the cost calculator reads the *KeywordAbility*, never a `Keyword.AFFINITY` enum entry, so the
 * printed keyword line is left for `CardBuilder.build()` to derive. Affinity shaves generic mana
 * only, flooring this at {U}.
 *
 * The trigger is the Thopter Architect / Perimeter Patrol shape: [Triggers.entersBattlefield] over
 * `Artifact.youControl()` with [TriggerBinding.ANY]. `ANY` rather than `OTHER` because the printed
 * text says "an artifact you control", not "*another* artifact" — the Whale is not itself an
 * artifact, so the binding never actually matters for its own entry, but it does mean an artifact
 * entering the same turn the Whale does still fires. The controller predicate lives on the filter,
 * so an opponent's artifact entering does not untap it. It is per-permanent: several artifacts
 * entering at once each fire it separately.
 *
 * "Untap this creature" targets nothing, so the effect points at [EffectTarget.Self] rather than the
 * triggering entity — the artifact that entered is the trigger's subject, the Whale is its object.
 */
val SteelfinWhale = card("Steelfin Whale") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    power = 3
    toughness = 4
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Whenever an artifact you control enters, untap this creature."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever an artifact you control enters, untap this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Milivoj Ćeran"
        flavorText = "It feeds on metal filings suspended in the water, sieving flecks of precious ore through magnetic baleen."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e7ca8b6-d7e0-4af2-a578-bf45a8731c19.jpg?1783926870"
    }
}
