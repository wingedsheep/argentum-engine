package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frostveil Ambush — Ikoria: Lair of Behemoths #52
 * {3}{U}{U} · Instant
 *
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step.
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 *
 * Same spell shape as Frost Breath: each target is tapped and gets DOESNT_UNTAP keyed to its own
 * controller's next untap step.
 */
val FrostveilAmbush = card("Frostveil Ambush") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures. Those creatures don't untap during their controller's next untap step.\nCycling {1} ({1}, Discard this card: Draw a card.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Johan Grenier"
        flavorText = "Hunters' methods vary from simple and brutal to elaborate and brutal."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deef1197-e40a-4c9d-919a-213f1bdf3e3e.jpg?1783931075"
        ruling("2020-04-17", "Frostveil Ambush can target a creature that's already tapped. That creature won't untap during its controller's next untap step.")
    }
}
