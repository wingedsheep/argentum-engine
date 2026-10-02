package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nyxborn Hydra — Modern Horizons 3 #164
 * {X}{G} · Enchantment Creature — Hydra · 0/1
 *
 * Either way it is cast — as a creature for {X}{G} or bestowed for {X}{G}{G} — the permanent
 * enters with the announced X as +1/+1 counters. Bestowed, the counters stay on the Aura and
 * feed the enchanted creature's bonus; once unattached it is an X/X+1 creature again.
 */
val NyxbornHydra = card("Nyxborn Hydra") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Hydra"
    power = 0
    toughness = 1
    oracleText = "Bestow {X}{G}{G} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Reach, trample\n" +
        "This permanent enters with X +1/+1 counters on it.\n" +
        "Enchanted creature gets +1/+1 for each +1/+1 counter on this Aura and has reach and trample."

    keywordAbility(KeywordAbility.bestow("{X}{G}{G}"))
    keywords(Keyword.REACH, Keyword.TRAMPLE)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    staticAbility {
        val plusOneCounters = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = plusOneCounters,
            toughnessBonus = plusOneCounters,
        )
    }
    staticAbility { ability = GrantKeyword(Keyword.REACH) }
    staticAbility { ability = GrantKeyword(Keyword.TRAMPLE) }

    metadata {
        ruling("2024-06-07", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2024-06-07", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2024-06-07", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Vincent Christiaens"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/902a969e-9f22-4e92-93eb-9d4536ca82e5.jpg?1783911257"
    }
}
