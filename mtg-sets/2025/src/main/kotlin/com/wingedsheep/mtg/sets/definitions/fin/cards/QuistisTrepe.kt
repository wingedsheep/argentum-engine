package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quistis Trepe — Final Fantasy #66
 * {2}{U} · Legendary Creature — Human Wizard · 2/2
 *
 * Blue Magic — When Quistis Trepe enters, you may cast target instant or sorcery card from a
 * graveyard, and mana of any type can be spent to cast that spell. If that spell would be put
 * into a graveyard, exile it instead.
 *
 * "Blue Magic" is an ability word (CR 207.2c) — flavor only, no rules meaning, so it adds no
 * keyword. The card targets an instant/sorcery in *any* graveyard (yours or an opponent's),
 * moves it to exile, then grants a may-play-from-exile permission with `withAnyManaType = true`
 * (the "mana of any type can be spent" clause — you still pay the cost) and
 * `insteadOfGraveyard = AfterResolveDestination.EXILE` (the "if it would be put into a graveyard, exile it instead"
 * rider). This mirrors Nita, Forum Conciliator's paid cast-from-exile, but as an ETB trigger
 * targeting any graveyard rather than an activated ability limited to opponents'. The "you may"
 * is honored by the cast itself being optional — the granted permission is never forced.
 */
val QuistisTrepe = card("Quistis Trepe") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Blue Magic — When Quistis Trepe enters, you may cast target instant or sorcery card " +
        "from a graveyard, and mana of any type can be spent to cast that spell. If that spell would be " +
        "put into a graveyard, exile it instead."

    triggeredAbility {
        val target = target(TargetFilter.InstantOrSorceryInGraveyard)
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            // Exile the targeted card from its graveyard.
            run(Effects.Move(target, Zone.EXILE))
            // Gather it into a named collection so the may-play grant can key off it.
            val borrowed = gather(CardSource.ChosenTargets)
            // "You may cast ... and mana of any type can be spent" + "if it would be put into a
            // graveyard, exile it instead."
            run(Effects.GrantMayPlayFromExile(
                from = borrowed,
                expiry = MayPlayExpiry.EndOfTurn,
                withAnyManaType = true,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
            ))
        }
        description = "Blue Magic — When Quistis Trepe enters, you may cast target instant or sorcery " +
            "card from a graveyard, and mana of any type can be spent to cast that spell. If that spell " +
            "would be put into a graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Touge369"
        flavorText = "\"It's not like everyone can get by on their own, you know?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61784cbd-92e9-43c7-a1a8-4004b1bf4dae.jpg?1748706001"
    }
}
