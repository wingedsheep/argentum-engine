package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kwain, Itinerant Meddler
 * {W}{U}
 * Legendary Creature — Rabbit Wizard
 * 1/3
 * {T}: Each player may draw a card, then each player who drew a card this way gains 1 life.
 *
 * Each player, in APNAP order ([Player.ActivePlayerFirst]), answers their own yes/no
 * ([Effects.May] with the iterated player as decision maker). "Who drew a card this way" is the
 * Sindbad peek: gather the top card of that player's library, draw, then keep it only if it
 * actually reached the hand — so an empty library, a replaced draw (dredge) or a draw cap
 * (Narset) earns no life. Known deviation: the iterations run one after another, so a player
 * gains their 1 life before the next player chooses, where the card has every player draw first
 * and then all who drew gain life.
 */
val KwainItinerantMeddler = card("Kwain, Itinerant Meddler") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Rabbit Wizard"
    power = 1
    toughness = 3
    oracleText = "{T}: Each player may draw a card, then each player who drew a card this way gains 1 life."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachPlayer(
            players = Player.ActivePlayerFirst,
            effect = Effects.May(
                decisionMaker = EffectTarget.Controller,
                prompt = "Draw a card? (If you do, you gain 1 life.)",
                effect = Effects.Pipeline {
                    val toDraw = gather(CardSource.TopOfLibrary(1))
                    run(Effects.DrawCards(1))
                    val drawn = filter(toDraw, GameObjectFilter.Any.currentlyIn(Zone.HAND))
                    ifNotEmpty(drawn) {
                        run(Effects.GainLife(1))
                    }
                },
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Lucas Graciano"
        flavorText = "\"Why so slow, why so slow? Hurry up! You never know what you'll find.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f571fef1-1fd2-4355-b803-5edccb6f4b94.jpg?1783928771"
    }
}
