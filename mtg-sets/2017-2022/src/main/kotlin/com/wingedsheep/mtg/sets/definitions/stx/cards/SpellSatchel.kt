package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spell Satchel — Strixhaven: School of Mages #258 (canonical printing)
 * {2} · Artifact
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, put a book counter on this
 * artifact.
 * {T}, Remove a book counter from this artifact: Add {C}.
 * {3}, {T}, Remove three book counters from this artifact: Draw a card.
 *
 * [CounterType.BOOK] is a passive store: magecraft fills it, and the two abilities spend it as a
 * cost ([Costs.RemoveCounterFromSelf]). The first is a mana ability (CR 605.1a).
 */
val SpellSatchel = card("Spell Satchel") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a book " +
        "counter on this artifact.\n" +
        "{T}, Remove a book counter from this artifact: Add {C}.\n" +
        "{3}, {T}, Remove three book counters from this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.BOOK, 1, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a " +
            "book counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.BOOK))
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}, Remove a book counter from this artifact: Add {C}."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.BOOK, 3)
        )
        effect = Effects.DrawCards(1)
        description = "{3}, {T}, Remove three book counters from this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "258"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7de826a0-6e68-4d40-a51c-8d6e7a4148d1.jpg?1783927279"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
    }
}
