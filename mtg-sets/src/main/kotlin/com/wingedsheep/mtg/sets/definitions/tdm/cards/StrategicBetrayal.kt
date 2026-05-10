package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Strategic Betrayal — Tarkir: Dragonstorm #94
 * {1}{B} · Sorcery
 *
 * Target opponent exiles a creature they control and their graveyard.
 *
 * Ruling: the opponent chooses which creature. If they control no creatures,
 * they simply exile their graveyard.
 */
val StrategicBetrayal = card("Strategic Betrayal") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent exiles a creature they control and their graveyard."

    spell {
        target("target opponent", Targets.Opponent)
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature,
                        player = Player.ContextPlayer(0)
                    ),
                    storeAs = "creaturesCanExile"
                ),
                ConditionalOnCollectionEffect(
                    collection = "creaturesCanExile",
                    ifNotEmpty = CompositeEffect(
                        listOf(
                            SelectFromCollectionEffect(
                                from = "creaturesCanExile",
                                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                                chooser = Chooser.TargetPlayer,
                                storeSelected = "chosenCreature",
                                prompt = "Choose a creature to exile",
                                useTargetingUI = true
                            ),
                            MoveCollectionEffect(
                                from = "chosenCreature",
                                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
                            )
                        )
                    )
                ),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    storeAs = "opponentGraveyard"
                ),
                MoveCollectionEffect(
                    from = "opponentGraveyard",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Flavio Greco Paglia"
        flavorText = "As Sultai blades threatened Qatros city walls, Mehtma of House Fenzala turned her weapons on her political rivals."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95617742-548d-464a-bb89-a858ffa9018f.jpg?1743204340"
        ruling("2025-04-04", "The opponent chooses which creature to exile as Strategic Betrayal resolves. If they don't control any creatures at that time, they simply exile their graveyard.")
    }
}
