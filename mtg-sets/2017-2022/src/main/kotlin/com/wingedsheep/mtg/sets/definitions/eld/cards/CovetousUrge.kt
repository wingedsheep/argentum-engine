package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Covetous Urge
 * {U/B}{U/B}{U/B}{U/B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from that player's graveyard or
 * hand and exile it. You may cast that card for as long as it remains exiled, and you may spend
 * mana as though it were mana of any color to cast that spell.
 *
 * Memory Leak's mixed-zone choice, then a permanent cast-only grant on the exiled card. If the
 * card is an Adventure cast this way, the engine's on-an-Adventure permission goes to the spell's
 * caster, matching the ruling that you, not the owner, may cast the creature later.
 */
val CovetousUrge = card("Covetous Urge") {
    manaCost = "{U/B}{U/B}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from that player's " +
        "graveyard or hand and exile it. You may cast that card for as long as it remains exiled, " +
        "and you may spend mana as though it were mana of any color to cast that spell."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val candidates = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any
                )
            )
            val toExile = chooseExactly(
                1,
                from = candidates,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card from their hand or graveyard to exile — you may cast it",
                alwaysPrompt = true,
                showAllCards = true
            )
            exile(toExile, owner = opponent.asPlayer)
            run(Effects.GrantMayPlayFromExile(
                from = toExile,
                expiry = MayPlayExpiry.Permanent,
                nonLandOnly = true,
                withAnyManaColor = true
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Carmen Sinek"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3b4871d-4d74-411b-88e0-9a9d386cafe1.jpg?1783932592"
        ruling("2019-10-04", "If you cast an Adventure with Covetous Urge, it's you, not the card's owner, who may cast the creature later.")
        ruling("2019-10-04", "Covetous Urge's effect doesn't change when you can cast the exiled card. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty.")
        ruling("2019-10-04", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
    }
}
