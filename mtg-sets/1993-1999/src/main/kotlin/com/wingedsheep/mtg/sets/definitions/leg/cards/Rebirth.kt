package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rebirth
 * {3}{G}{G}{G}
 * Sorcery
 * Remove this card from your deck before playing if you're not playing for ante.
 * Each player may ante the top card of their library. If a player does, that player's life total
 * becomes 20.
 *
 * Only legal in a game played for ante (CR 407.3, enforced by deck validation). Each player decides
 * in APNAP order, and only for their own library (only a card's owner can ante it, CR 407.4). A
 * player with an empty library has nothing to ante, so isn't asked and keeps their life total.
 */
val Rebirth = card("Rebirth") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "Each player may ante the top card of their library. If a player does, that player's life total " +
        "becomes 20."

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.May(
                effect = Effects.Pipeline {
                    val top = gather(CardSource.TopOfLibrary(1))
                    ifNotEmpty(top) {
                        move(top, CardDestination.ToZone(Zone.ANTE))
                        run(Effects.SetLifeTotal(20, EffectTarget.Controller))
                    }
                },
                decisionMaker = EffectTarget.Controller,
                feasibility = FeasibilityCheck.HasCardsInZone(Zone.LIBRARY),
                prompt = "Ante the top card of your library to have your life total become 20?",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0d3ce01-e344-4608-b709-320be3600019.jpg?1783948045"
    }
}
