package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cabal Interrogator
 * {1}{B}
 * Creature — Zombie Wizard
 * 1/1
 * {X}{B}, {T}: Target player reveals X cards from their hand and you choose one of them.
 * That player discards that card. Activate only as a sorcery.
 */
val CabalInterrogator = card("Cabal Interrogator") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie Wizard"
    power = 1
    toughness = 1
    oracleText = "{X}{B}, {T}: Target player reveals X cards from their hand and you choose one of them. That player discards that card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{B}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        target = TargetPlayer()
        effect = CompositeEffect(
            listOf(
                // 1. Gather all cards from target player's hand
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                // 2. Target player chooses X cards to reveal (auto-selects all if ≤X, skips if empty)
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.XValue),
                    chooser = Chooser.TargetPlayer,
                    storeSelected = "revealed"
                ),
                // 3. Controller chooses 1 to discard
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "toDiscard"
                ),
                // 4. Move chosen card to target player's graveyard
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/256a7a37-6f47-47a3-b149-5692aee8b34a.jpg?1562526599"
    }
}
