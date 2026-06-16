package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Patient Naturalist
 * {2}{G}
 * Creature — Human Scout
 * 2/3
 *
 * When this creature enters, mill three cards. Put a land card from among the milled cards
 * into your hand. If you can't, create a Treasure token.
 *
 * Modeled as an inline Gather → Move (mill) → partition pipeline: the top three cards go to
 * the graveyard, the milled collection is partitioned on `Land`, and the land partition is
 * what's offered to hand. If no land was among the milled cards (`ifNotEmpty` on the land
 * partition is empty), a Treasure is created instead — matching "if you can't, create a
 * Treasure token." The gathered collection tracks entity refs, so selecting from it after
 * the mill move still points at exactly those three cards now in the graveyard.
 */
val PatientNaturalist = card("Patient Naturalist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, mill three cards. Put a land card from among the " +
        "milled cards into your hand. If you can't, create a Treasure token. (To mill three " +
        "cards, put the top three cards of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            val milled = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(3), Player.You))
            toGraveyard(milled)
            ifNotEmpty(milled, filter = GameObjectFilter.Land) {
                val lands = filter(milled, GameObjectFilter.Land)
                val chosen = chooseExactly(1, from = lands)
                toHand(chosen)
            } orElse {
                run(Effects.CreateTreasure(1))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Inka Schulz"
        flavorText = "The Atiin delight in delving into the unique wonders of each world they visit."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dd17cea-9e8c-4dba-b6ab-a6b9de87a306.jpg?1712355965"
    }
}
