package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Khalni Heart Expedition
 * {1}{G}
 * Enchantment
 * Landfall — Whenever a land you control enters, you may put a quest counter on this enchantment.
 * Remove three quest counters from this enchantment and sacrifice it: Search your library for up
 * to two basic land cards, put them onto the battlefield tapped, then shuffle.
 */
val KhalniHeartExpedition = card("Khalni Heart Expedition") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, you may put a quest counter on this enchantment.\n" +
        "Remove three quest counters from this enchantment and sacrifice it: Search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        optional = true
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.RemoveCounterFromSelf(CounterType.QUEST, 3),
            Costs.SacrificeSelf,
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/797d8cd6-fc6b-4560-9c6f-1a867dfce85a.jpg?1783942135"
        ruling(
            "2024-11-08",
            "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.",
        )
        ruling(
            "2024-11-08",
            "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.",
        )
    }
}
