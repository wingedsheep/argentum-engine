package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Last Light of Durin's Day
 * {1}{R}
 * Enchantment
 *
 * Whenever a Mountain you control enters, put a quest counter on this enchantment. If it has six or
 * more quest counters on it, sacrifice it. If you do, search your hand and/or library for a Dragon
 * card and put it onto the battlefield. If you search your library this way, shuffle.
 * Mountaincycling {2}
 *
 * Modeling notes — pure composition, no new engine vocabulary:
 *  - **Trigger** — "a Mountain you control" is any *land* with the Mountain subtype (a nonbasic dual
 *    with the type counts), so the filter is `Land.withSubtype(MOUNTAIN).youControl()` with an `ANY`
 *    binding rather than a basic-land-only filter.
 *  - **Threshold** — the counter goes on unconditionally, then a [ConditionalEffect] gated on the
 *    *live* count ([Conditions.SourceCounterCountAtLeast]`(QUEST, 6)`) fires the payoff, exactly
 *    like the Ascension cycle. "Six or more" (not "exactly six") matters because proliferate can
 *    overshoot six between triggers.
 *  - **Search** — [Patterns.Library.searchMultipleZones] over `HAND` + `LIBRARY` is the "search your
 *    hand and/or library" shape (Fang-Druid Summoner's library+graveyard sibling); it shuffles and
 *    emits the searched-library event only because `LIBRARY` is among the zones, which is exactly
 *    what the printed "If you search your library this way, shuffle" rider says. "Dragon **card**"
 *    is `Any.withSubtype(DRAGON)`, not `Creature.withSubtype(...)` — the text asks for the subtype,
 *    not the card type.
 *  - **Mountaincycling {2}** — the generic [KeywordAbility.typecycling] with the Mountain land type.
 *
 * Edge cases: the search is not optional in wording, but `searchMultipleZones` uses a choose-up-to-1
 * selection, which is how the engine expresses the always-legal "fail to find" (CR 701.19c) — a
 * player with no Dragon simply finds nothing. The sacrifice is sequenced before the search so the
 * enchantment is already gone when the Dragon arrives; if the enchantment has left the battlefield
 * before this trigger resolves, the counter count reads zero and the payoff never starts.
 */
val LastLightOfDurinsDay = card("Last Light of Durin's Day") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a Mountain you control enters, put a quest counter on this enchantment. " +
        "If it has six or more quest counters on it, sacrifice it. If you do, search your hand " +
        "and/or library for a Dragon card and put it onto the battlefield. If you search your " +
        "library this way, shuffle.\n" +
        "Mountaincycling {2} ({2}, Discard this card: Search your library for a Mountain card, " +
        "reveal it, put it into your hand, then shuffle.)"

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN).youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.Composite(
            Effects.AddCounters(Counters.QUEST, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.QUEST, 6),
                effect = Effects.Composite(
                    Effects.SacrificeTarget(EffectTarget.Self),
                    Patterns.Library.searchMultipleZones(
                        zones = listOf(Zone.HAND, Zone.LIBRARY),
                        filter = GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
                        count = 1,
                        destination = SearchDestination.BATTLEFIELD
                    )
                )
            )
        )
        description = "Whenever a Mountain you control enters, put a quest counter on this " +
            "enchantment. If it has six or more quest counters on it, sacrifice it. If you do, " +
            "search your hand and/or library for a Dragon card and put it onto the battlefield. " +
            "If you search your library this way, shuffle."
    }

    keywordAbility(KeywordAbility.typecycling("Mountain", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Harkalé Linaï"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df29484b-de4b-4bab-995a-7605745780d9.jpg?1784798201"
    }
}
