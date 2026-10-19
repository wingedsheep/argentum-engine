package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Containment Construct — Kamigawa: Neon Dynasty #243 (canonical printing)
 * {2} · Artifact Creature — Construct · 2/1
 *
 * Whenever you discard a card, you may exile that card from your graveyard. If you do, you may
 * play that card this turn.
 *
 * `Triggers.you.discards()` fires once per discarded card and binds it as the triggering entity.
 * The exile is a tracked move filtered to cards currently in a graveyard, so a card that left the
 * graveyard before the trigger resolves stays where it is ("from your graveyard"), and the play
 * permission goes only to the card actually exiled ("if you do"). Playing it still costs its mana;
 * the permission ends at cleanup this turn.
 */
val ContainmentConstruct = card("Containment Construct") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 1
    oracleText = "Whenever you discard a card, you may exile that card from your graveyard. " +
        "If you do, you may play that card this turn."

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.May(
            Effects.Pipeline {
                val discarded = gather(CardSource.TriggeringEntity)
                val exiled = moveTracked(
                    discarded,
                    CardDestination.ToZone(Zone.EXILE),
                    filter = GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD),
                )
                run(Effects.GrantMayPlayFromExile(exiled, MayPlayExpiry.EndOfTurn))
            },
            prompt = "Exile the discarded card? You may play it this turn."
        )
        description = "Whenever you discard a card, you may exile that card from your graveyard. " +
            "If you do, you may play that card this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Julian Kok Joon Wen"
        flavorText = "Automated units are deployed to stabilize merge sites deemed too dangerous " +
            "even for trained intercessors."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/520e5505-429b-4da0-b25e-14b8d4e81ce3.jpg?1783923826"
    }
}
