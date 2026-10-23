package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ardent Dustspeaker — Strixhaven: School of Mages #92 (canonical printing)
 * {4}{R} · Creature — Minotaur Shaman · 3/4
 *
 * Whenever this creature attacks, you may put an instant or sorcery card from your graveyard on
 * the bottom of your library. If you do, exile the top two cards of your library. You may play
 * those cards this turn.
 *
 * The "you may put … If you do" half is Volcanic Spite's shape: [Effects.IfYouDo] over an
 * up-to-one graveyard choice moved to the library bottom, so declining (or an empty graveyard)
 * skips the payoff. The payoff is Wild Wasteland's impulse pipeline — exile the top two, then
 * [Effects.GrantMayPlayFromExile] for this turn (the permission outlives the Dustspeaker).
 */
val ArdentDustspeaker = card("Ardent Dustspeaker") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Shaman"
    oracleText = "Whenever this creature attacks, you may put an instant or sorcery card from your " +
        "graveyard on the bottom of your library. If you do, exile the top two cards of your library. " +
        "You may play those cards this turn."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val graveyard = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery)
                )
                val chosen = chooseUpTo(
                    1,
                    from = graveyard,
                    prompt = "You may put an instant or sorcery card from your graveyard on the bottom of your library"
                )
                toLibraryBottom(chosen)
            },
            then = Effects.Pipeline {
                val exiledCards = gather(CardSource.TopOfLibrary(2))
                exile(exiledCards)
                run(Effects.GrantMayPlayFromExile(exiledCards))
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Mads Ahm"
        flavorText = "\"The Founder Dragons weren't even hatched when this scroll was written!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7ff4832a-26c0-4da5-abbb-1941e7084e72.jpg?1783927358"
        ruling("4/16/2021", "You may play those cards that turn even if Ardent Dustspeaker leaves the battlefield or another player gains control of it.")
        ruling("4/16/2021", "Any cards exiled with Ardent Dustspeaker that you don't play remain exiled. You will not be able to play them on later turns, even if Ardent Dustspeaker's ability triggers again.")
    }
}
