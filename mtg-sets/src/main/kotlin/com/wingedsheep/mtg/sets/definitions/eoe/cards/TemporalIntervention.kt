package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Temporal Intervention
 * {2}{B}
 * Sorcery
 * Void — This spell costs {2} less to cast if a nonland permanent left the battlefield this turn
 *   or a spell was warped this turn.
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.
 */
val TemporalIntervention = card("Temporal Intervention") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Void — This spell costs {2} less to cast if a nonland permanent left the battlefield this turn or a spell was warped this turn.\n" +
        "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfVoid(2)
            )
        )
    }

    spell {
        val t = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            listOf(
                RevealHandEffect(t),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    storeSelected = "toDiscard",
                    prompt = "Choose a nonland card to discard",
                    alwaysPrompt = true,
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Chris Rallis"
        flavorText = "It will not end any other way than this."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79f9525a-4cb7-411e-b7b5-2113e93bcbc3.jpg?1753352166"
    }
}
