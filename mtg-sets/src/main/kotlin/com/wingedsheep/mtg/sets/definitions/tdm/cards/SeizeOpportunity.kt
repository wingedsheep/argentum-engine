package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Seize Opportunity
 * {2}{R}
 * Instant
 *
 * Choose one —
 * • Exile the top two cards of your library. Until the end of your next turn, you may play those cards.
 * • Up to two target creatures each get +2/+1 until end of turn.
 */
val SeizeOpportunity = card("Seize Opportunity") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Exile the top two cards of your library. Until the end of your next turn, you may play those cards.\n" +
        "• Up to two target creatures each get +2/+1 until end of turn."

    spell {
        effect = ModalEffect.chooseOne(
            // Exile top two and play until end of next turn.
            Mode.noTarget(
                effect = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                            storeAs = "exiledCards"
                        ),
                        MoveCollectionEffect(
                            from = "exiledCards",
                            destination = CardDestination.ToZone(Zone.EXILE)
                        ),
                        GrantMayPlayFromExileEffect("exiledCards", MayPlayExpiry.UntilEndOfNextTurn)
                    )
                ),
                description = "Exile the top two cards of your library. Until the end of your next turn, you may play those cards"
            ),
            // Up to two target creatures each get +2/+1 until end of turn.
            Mode(
                effect = ForEachTargetEffect(
                    listOf(Effects.ModifyStats(2, 1, EffectTarget.ContextTarget(0)))
                ),
                targetRequirements = listOf(Targets.UpToCreatures(2)),
                description = "Up to two target creatures each get +2/+1 until end of turn"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Josiah \"Jo\" Cameron"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7818d28-b9a5-4341-9adc-666070b8878d.jpg?1743204441"
    }
}
