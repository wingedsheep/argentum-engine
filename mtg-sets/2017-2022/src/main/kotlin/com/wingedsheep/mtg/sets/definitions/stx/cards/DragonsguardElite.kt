package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragonsguard Elite — Strixhaven: School of Mages #127 (canonical printing)
 * {1}{G} · Creature — Human Druid · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, put a +1/+1 counter on this
 * creature.
 * {4}{G}{G}: Double the number of +1/+1 counters on this creature.
 *
 * The doubling is [Effects.DoubleCounters]: it puts as many +1/+1 counters as are already there,
 * so placement replacements (Hardened Scales) see an ordinary placement, per the ruling.
 */
val DragonsguardElite = card("Dragonsguard Elite") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a +1/+1 " +
        "counter on this creature.\n" +
        "{4}{G}{G}: Double the number of +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a " +
            "+1/+1 counter on this creature."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}{G}")
        effect = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.Self)
        description = "{4}{G}{G}: Double the number of +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "David Rapoza"
        flavorText = "\"The Founder Dragons brought the Blood Age to an end. It is an honor to serve them.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/658ec634-3eb2-4967-b938-d20d31ab77e3.jpg?1783927344"
        ruling("2021-04-16", "To double the number of +1/+1 counters on a permanent, put a number of +1/+1 counters on it equal to the number it already has. Other cards that interact with putting counters on it will interact with this effect accordingly.")
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
    }
}
