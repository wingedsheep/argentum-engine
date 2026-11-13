package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bane of Bala Ged
 * {7}
 * Creature — Eldrazi
 * 7/5
 *
 * Whenever this creature attacks, defending player exiles two permanents they control.
 *
 * The exile twin of annihilator: an untargeted attack trigger whose pipeline gathers every
 * permanent the defending player controls and has *that player* ([Chooser.DefendingPlayer]) pick
 * exactly two to exile. With two or fewer permanents the selection is forced, so whatever they
 * control is exiled (ruling); a planeswalker being attacked is a legal pick.
 */
val BaneOfBalaGed = card("Bane of Bala Ged") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 7
    toughness = 5
    oracleText = "Whenever this creature attacks, defending player exiles two permanents they control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val permanents = gather(
                CardSource.FromZone(Zone.BATTLEFIELD, Player.DefendingPlayer, GameObjectFilter.Permanent)
            )
            val chosen = chooseExactly(
                2,
                from = permanents,
                chooser = Chooser.DefendingPlayer,
                prompt = "Choose two permanents you control to exile",
                useTargetingUI = true,
            )
            exile(chosen, owner = Player.DefendingPlayer)
        }
        description = "Whenever this creature attacks, defending player exiles two permanents they control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Chase Stone"
        flavorText = "The continent of Bala Ged was ravaged by massive Eldrazi of Ulamog's lineage, " +
            "its lush jungles and cascading rivers reduced to dust."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3741e62a-4b86-46ff-a7df-a8ceaf3b9a0c.jpg?1783938225"
        ruling(
            "2015-08-25",
            "If Bane of Bala Ged attacks a planeswalker, the defending player may exile that planeswalker. " +
                "Bane of Bala Ged will still be attacking and may be blocked as normal. If it's not blocked, " +
                "it won't deal combat damage."
        )
        ruling("2015-08-25", "The permanents are exiled before blockers are chosen.")
        ruling("2015-08-25", "If the defending player controls only one permanent, that permanent will be exiled.")
    }
}
