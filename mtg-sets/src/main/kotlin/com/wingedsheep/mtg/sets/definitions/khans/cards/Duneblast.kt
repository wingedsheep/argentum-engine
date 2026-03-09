package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Duneblast
 * {4}{W}{B}{G}
 * Sorcery
 * Choose up to one creature. Destroy the rest.
 *
 * The choice doesn't target. If you don't choose a creature, all creatures are destroyed.
 */
val Duneblast = card("Duneblast") {
    manaCost = "{4}{W}{B}{G}"
    typeLine = "Sorcery"
    oracleText = "Choose up to one creature. Destroy the rest."

    spell {
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature),
                storeAs = "all_creatures"
            ),
            SelectFromCollectionEffect(
                from = "all_creatures",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "saved",
                storeRemainder = "to_destroy",
                prompt = "Choose up to one creature to save",
                selectedLabel = "Save",
                remainderLabel = "Destroy"
            ),
            MoveCollectionEffect(
                from = "to_destroy",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Ryan Alexander Lee"
        flavorText = "The Abzan turn to this spell only as a last resort, for its inevitable result is what they most dread: to be alone."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d076ea5f-630a-4670-9b15-29fd2cececd2.jpg?1562793871"
        ruling("2014-09-20", "You decide which creature to spare as Duneblast resolves. This choice doesn't target the creature. If you don't choose a creature, then all creatures will be destroyed.")
    }
}
