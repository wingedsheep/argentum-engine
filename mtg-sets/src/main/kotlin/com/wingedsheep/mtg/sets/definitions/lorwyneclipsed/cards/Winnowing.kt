package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Winnowing
 * {4}{W}{W}
 * Sorcery
 *
 * Convoke
 * For each player, you choose a creature that player controls. Then each player
 * sacrifices all other creatures they control that don't share a creature type
 * with the chosen creature they control.
 */
val Winnowing = card("Winnowing") {
    manaCost = "{4}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "For each player, you choose a creature that player controls. Then each player sacrifices all other creatures they control that don't share a creature type with the chosen creature they control."

    keywords(Keyword.CONVOKE)

    spell {
        effect = ForEachPlayerEffect(
            players = Player.ActivePlayerFirst,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature
                    ),
                    storeAs = "playerCreatures"
                ),
                SelectFromCollectionEffect(
                    from = "playerCreatures",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.SourceController,
                    storeSelected = "chosen",
                    storeRemainder = "rest",
                    prompt = "Choose a creature this player controls",
                    useTargetingUI = true
                ),
                GatherSubtypesEffect(from = "chosen", storeAs = "chosenSubtypes"),
                FilterCollectionEffect(
                    from = "rest",
                    filter = CollectionFilter.MatchesFilter(
                        GameObjectFilter.Creature.withSubtypeInEachStoredGroup("chosenSubtypes")
                    ),
                    storeMatching = "shareTypes",
                    storeNonMatching = "noShareTypes"
                ),
                MoveCollectionEffect(
                    from = "noShareTypes",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f943a7d8-9550-427e-8c45-ef834329d345.jpg?1767659128"
    }
}
