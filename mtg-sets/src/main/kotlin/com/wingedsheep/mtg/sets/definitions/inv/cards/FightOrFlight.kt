package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fight or Flight
 * {3}{W}
 * Enchantment
 * At the beginning of combat on each opponent's turn, separate all creatures
 * that player controls into two piles. Only creatures in the pile of their
 * choice can attack this turn.
 *
 * A "divvy" (CR 700.3) restriction: you (the ability's controller) partition the
 * active opponent's creatures, that player chooses which pile can attack, and the
 * other pile can't attack this turn. Composed from the pile primitives plus a
 * resolution-time snapshot restriction — ForEachInCollection over the non-chosen
 * pile applies a single-target [Effects.CantAttack] to each creature, so creatures
 * entering after the split are unaffected.
 */
val FightOrFlight = card("Fight or Flight") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on each opponent's turn, separate all creatures that player controls into two piles. Only creatures in the pile of their choice can attack this turn."

    triggeredAbility {
        trigger = Triggers.phase(Step.BEGIN_COMBAT, Player.EachOpponent)
        effect = Effects.Composite(
            listOf(
                // 1. Gather the creatures the active opponent controls.
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.TriggeringPlayer,
                        filter = GameObjectFilter.Creature
                    ),
                    storeAs = "creatures"
                ),
                // 2. You separate that player's creatures into two piles.
                SelectFromCollectionEffect(
                    from = "creatures",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = Chooser.Controller,
                    storeSelected = "pileA",
                    storeRemainder = "pileB",
                    selectedLabel = "Pile 1",
                    remainderLabel = "Pile 2",
                    prompt = "Separate the active player's creatures into two piles. The creatures you select form Pile 1; the rest form Pile 2.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                ),
                // 3. That player chooses which pile may attack.
                ChoosePileEffect(
                    pileA = "pileA",
                    pileB = "pileB",
                    pileALabel = "Pile 1",
                    pileBLabel = "Pile 2",
                    chooser = Chooser.Opponent,
                    storeChosenAs = "canAttack",
                    storeOtherAs = "cantAttack",
                    prompt = "Choose which pile of your creatures can attack this turn."
                ),
                // 4. Only the chosen pile can attack — the other pile can't attack this turn.
                ForEachInCollectionEffect(
                    collection = "cantAttack",
                    effect = Effects.CantAttack(EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46bde162-3737-4b93-a27a-63b909a4183d.jpg?1562909374"
    }
}
