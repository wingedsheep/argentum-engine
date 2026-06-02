package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.dsl.Effects

/**
 * Tsabo's Decree
 * {5}{B}
 * Instant
 * Choose a creature type. Target player reveals their hand and discards all creature cards
 * of that type. Then destroy all creatures of that type that player controls. They can't be
 * regenerated.
 *
 * Composed entirely from existing primitives: [ChooseCreatureTypeEffect] stamps the chosen
 * type into the pipeline (`chosenValues["chosenCreatureType"]`); the hand discard gathers the
 * target player's creature cards and keeps only those matching the chosen type
 * (`matchChosenCreatureType`); and the board wipe iterates creatures of that type the target
 * player controls (`GroupFilter.chosenSubtypeKey`), destroying each without regeneration.
 */
val TsabosDecree = card("Tsabo's Decree") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose a creature type. Target player reveals their hand and discards all " +
        "creature cards of that type. Then destroy all creatures of that type that player " +
        "controls. They can't be regenerated."

    spell {
        val targetPlayer = target("target player", TargetPlayer())
        effect = Effects.Composite(
            listOf(
                ChooseCreatureTypeEffect,
                // Target player reveals their hand and discards all creature cards of that type.
                RevealHandEffect(targetPlayer),
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.HAND,
                        player = Player.ContextPlayer(0),
                        filter = GameObjectFilter.Creature,
                    ),
                    storeAs = "tsaboHand",
                ),
                SelectFromCollectionEffect(
                    from = "tsaboHand",
                    selection = SelectionMode.All,
                    matchChosenCreatureType = true,
                    storeSelected = "tsaboDiscard",
                ),
                MoveCollectionEffect(
                    from = "tsaboDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard,
                ),
                // Then destroy all creatures of that type that player controls; no regeneration.
                Effects.ForEachInGroup(
                    filter = GroupFilter(
                        baseFilter = GameObjectFilter.Creature.targetPlayerControls(),
                        chosenSubtypeKey = "chosenCreatureType",
                    ),
                    effect = Effects.Composite(
                        listOf(
                            CantBeRegeneratedEffect(EffectTarget.Self),
                            Effects.Move(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true),
                        ),
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c1a0ebd-1add-49e6-b5e6-5b26abb1de88.jpg?1562897461"
    }
}
