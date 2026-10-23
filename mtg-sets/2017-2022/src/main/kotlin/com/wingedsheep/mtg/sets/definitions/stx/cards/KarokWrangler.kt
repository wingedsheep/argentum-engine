package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Karok Wrangler — Strixhaven: School of Mages #135 (canonical printing)
 * {4}{G} · Creature — Elf Druid · 3/3
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, put a +1/+1 counter on
 * target creature you control.
 *
 * The magecraft trigger ([Triggers.you] `castsOrCopies` an instant or sorcery) carrying a
 * "target creature you control" and a single +1/+1 counter.
 */
val KarokWrangler = card("Karok Wrangler") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 3
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a +1/+1 " +
        "counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a " +
            "+1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Dan Murayama Scott"
        flavorText = "\"She showed up to class on a what?\"\n—Valentin, Witherbloom Dean"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74729320-f6ee-4176-9463-397d6e477d7a.jpg?1783927342"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
    }
}
