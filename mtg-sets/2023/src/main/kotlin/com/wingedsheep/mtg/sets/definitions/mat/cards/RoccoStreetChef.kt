package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Rocco, Street Chef — March of the Machine: The Aftermath #44
 * {R}{G}{W} · Legendary Creature — Elf Druid · 2/4
 *
 * At the beginning of your end step, each player exiles the top card of their library. Until your
 * next end step, each player may play the card they exiled this way.
 * Whenever a player plays a land from exile or casts a spell from exile, you put a +1/+1 counter on
 * target creature and create a Food token.
 *
 * `ForEachPlayer` rebinds "you" to each player for the exile and the grant, while the window's
 * "your next end step" stays keyed to Rocco's controller (the activating player), so every
 * player's card stops being playable as Rocco's controller's next end step begins — just before
 * this trigger exiles the next round.
 */
val RoccoStreetChef = card("Rocco, Street Chef") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Elf Druid"
    power = 2
    toughness = 4
    oracleText = "At the beginning of your end step, each player exiles the top card of their library. " +
        "Until your next end step, each player may play the card they exiled this way.\n" +
        "Whenever a player plays a land from exile or casts a spell from exile, you put a +1/+1 counter " +
        "on target creature and create a Food token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.Pipeline {
                val exiled = gather(CardSource.TopOfLibrary(1, Player.You))
                exile(exiled)
                run(Effects.GrantMayPlayFromExile(from = exiled, expiry = MayPlayExpiry.UntilNextEndStep))
            }
        )
        description = "At the beginning of your end step, each player exiles the top card of their library. " +
            "Until your next end step, each player may play the card they exiled this way."
    }

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.anyPlayer.playsLandFrom(Zone.EXILE),
            Triggers.anyPlayer.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.EXILE)))
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then Effects.CreateFood()
        description = "Whenever a player plays a land from exile or casts a spell from exile, you put a " +
            "+1/+1 counter on target creature and create a Food token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdb53ce7-845c-4c62-98a9-4fc33c67a07b.jpg?1783916509"
        ruling(
            "2023-05-12",
            "Unless an effect allows you to play additional lands that turn, you can play a land card you " +
                "exiled with Rocco only if you haven't played a land yet that turn."
        )
        ruling(
            "2023-05-12",
            "A player may play the card they exiled with Rocco even if Rocco leaves the battlefield before " +
                "the duration expires."
        )
    }
}
