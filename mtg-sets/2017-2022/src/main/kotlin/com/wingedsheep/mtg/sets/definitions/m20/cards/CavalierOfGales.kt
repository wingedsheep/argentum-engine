package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavalier of Gales — Core Set 2020 #52
 * {2}{U}{U}{U} · Creature — Elemental Knight 5/5
 *
 * Flying
 * When this creature enters, draw three cards, then put two cards from your hand on top of your
 * library in any order.
 * When this creature dies, shuffle it into its owner's library, then scry 2.
 *
 * The enters trigger is Brainstorm's pipeline. The dies trigger is Alabaster Dragon's shuffle-self
 * move followed by an unconditional scry — current Oracle text has no "if you do", so the scry
 * happens even if the card already left the graveyard.
 */
val CavalierOfGales = card("Cavalier of Gales") {
    manaCost = "{2}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Knight"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, draw three cards, then put two cards from your hand on top of your " +
        "library in any order.\n" +
        "When this creature dies, shuffle it into its owner's library, then scry 2. (Look at the top two " +
        "cards of your library, then put any number of them on the bottom and the rest on top in any order.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3) then
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val putBack = chooseExactly(2, hand)
                toLibraryTop(putBack)
            }
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Move(EffectTarget.Self, Zone.LIBRARY, ZonePlacement.Shuffled) then
            Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "52"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5356b11-e505-4838-aecb-327689eeead1.jpg?1783933012"
    }
}
