package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tome of Legends — Throne of Eldraine #332
 * {2} · Artifact — Book
 * This artifact enters with a page counter on it.
 * Whenever your commander enters or attacks, put a page counter on this artifact.
 * {1}, {T}, Remove a page counter from this artifact: Draw a card.
 *
 * "Your commander" is the commander of the deck you own (CR 903.3), hence
 * `commander().ownedByYou()` — the same shape as Vivi's Persistence. In a game without
 * commanders the trigger simply never fires, which is the correct rules outcome.
 */
val TomeOfLegends = card("Tome of Legends") {
    manaCost = "{2}"
    typeLine = "Artifact — Book"
    oracleText = "This artifact enters with a page counter on it.\n" +
        "Whenever your commander enters or attacks, put a page counter on this artifact.\n" +
        "{1}, {T}, Remove a page counter from this artifact: Draw a card."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PAGE,
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.a(GameObjectFilter.Any.commander().ownedByYou()).enters(),
            Triggers.a(GameObjectFilter.Any.commander().ownedByYou()).attacks(),
        )
        effect = Effects.AddCounters(CounterType.PAGE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.PAGE, 1))
        effect = Effects.DrawCards(1)
        description = "{1}, {T}, Remove a page counter from this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "332"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/040301e8-20c1-4f4c-8766-d05f11415efd.jpg?1783932546"
    }
}
