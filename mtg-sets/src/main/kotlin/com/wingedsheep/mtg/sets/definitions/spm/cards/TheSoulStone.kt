package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Soul Stone (Marvel's Spider-Man, #66)
 * {1}{B}
 * Legendary Artifact — Infinity Stone
 *
 * Indestructible
 * {T}: Add {B}.
 * {6}{B}, {T}, Exile a creature you control: Harness The Soul Stone. (Once harnessed, its ∞
 * ability is active.)
 * ∞ — At the beginning of your upkeep, return target creature card from your graveyard to the
 * battlefield.
 *
 * Implementation (composition-first — the only new vocabulary is the `harness` marker counter):
 *  - **Harness** is modeled as a binary marker counter ([Counters.HARNESS]). The Harness activated
 *    ability places one; the `∞` triggered ability is gated on the Stone having a harness counter
 *    ([Conditions.SourceHasCounter]), so it does nothing until harnessed and reactivates every
 *    upkeep thereafter. A counter (not a durable component) matches the flavor: it resets if the
 *    Stone leaves the battlefield, and re-placing it is idempotent.
 *  - `{T}: Add {B}` is a mana ability; the Harness cost pairs `{6}{B}` + tap + an exile-a-creature
 *    additional cost ([Costs.ExilePermanents]); the `∞` reanimates a targeted creature card from
 *    your graveyard ([Effects.PutOntoBattlefield]).
 */
val TheSoulStone = card("The Soul Stone") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Infinity Stone"
    oracleText = "Indestructible\n" +
        "{T}: Add {B}.\n" +
        "{6}{B}, {T}, Exile a creature you control: Harness The Soul Stone. (Once harnessed, its " +
        "∞ ability is active.)\n" +
        "∞ — At the beginning of your upkeep, return target creature card from your graveyard to " +
        "the battlefield."

    keywords(Keyword.INDESTRUCTIBLE)

    // {T}: Add {B}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
    }

    // {6}{B}, {T}, Exile a creature you control: Harness The Soul Stone.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{6}{B}"),
            Costs.Tap,
            Costs.ExilePermanents(GameObjectFilter.Creature.youControl(), minCount = 1, excludeSelf = false)
        )
        effect = Effects.AddCounters(Counters.HARNESS, 1, EffectTarget.Self)
        description = "{6}{B}, {T}, Exile a creature you control: Harness The Soul Stone."
    }

    // ∞ — At the beginning of your upkeep (once harnessed), return target creature card from your
    // graveyard to the battlefield.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.HARNESS))
        val graveyardCreature = target("target creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.PutOntoBattlefield(graveyardCreature)
        description = "∞ — At the beginning of your upkeep, return target creature card from your " +
            "graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "66"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1982f910-a9bd-4e94-a187-84381b22aacc.jpg?1783905342"
    }
}
