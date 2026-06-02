package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Riku of Many Paths
 * {G}{U}{R}
 * Legendary Creature — Human Wizard
 * 3/3
 *
 * Whenever you cast a modal spell, choose up to X, where X is the number of times
 * you chose a mode for that spell —
 * • Exile the top card of your library. Until the end of your next turn, you may play it.
 * • Put a +1/+1 counter on Riku. It gains trample until end of turn.
 * • Create a 1/1 blue Bird creature token with flying.
 *
 * Implementation:
 * - Trigger: `Triggers.youCastSpell(requires = setOf(SpellCastPredicate.IsModal))`
 *   — fires only when the cast spell had at least one chosen mode.
 * - X plumbing: the engine's [com.wingedsheep.engine.core.SpellCastEvent] now carries
 *   `chosenModesCount`, which becomes [DynamicAmount.ContextProperty] with key
 *   [ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL] inside this trigger's effect.
 * - "Choose up to X": [ModalEffect.chooseUpToDynamic] resolves the cap at trigger
 *   resolution and uses the standard modal continuation. Rulings: modes can't be
 *   repeated each time Riku triggers, so [ModalEffect.allowRepeat] stays `false`.
 */
val RikuOfManyPaths = card("Riku of Many Paths") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast a modal spell, choose up to X, where X is the number " +
        "of times you chose a mode for that spell —\n" +
        "• Exile the top card of your library. Until the end of your next turn, you may play it.\n" +
        "• Put a +1/+1 counter on Riku. It gains trample until end of turn.\n" +
        "• Create a 1/1 blue Bird creature token with flying."

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.IsModal)
        )
        effect = ModalEffect.chooseUpToDynamic(
            dynamicMax = DynamicAmount.ContextProperty(
                ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL
            ),
            // Mode 1 — impulse-draw with extended window.
            Mode.noTarget(
                Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                            storeAs = "rikuExile"
                        ),
                        MoveCollectionEffect(
                            from = "rikuExile",
                            destination = CardDestination.ToZone(Zone.EXILE)
                        ),
                        GrantMayPlayFromExileEffect(
                            "rikuExile",
                            MayPlayExpiry.UntilEndOfNextTurn
                        )
                    )
                ),
                description = "Exile the top card of your library. Until the end of your next turn, you may play it."
            ),
            // Mode 2 — +1/+1 counter + trample until end of turn.
            Mode.noTarget(
                Effects.Composite(
                    listOf(
                        Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                        Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.EndOfTurn)
                    )
                ),
                description = "Put a +1/+1 counter on Riku. It gains trample until end of turn."
            ),
            // Mode 3 — create a 1/1 blue Bird with flying.
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Bird"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/0/0/000d9280-a79a-4f9f-822c-7aaecbff3337.jpg?1712316187"
                ),
                description = "Create a 1/1 blue Bird creature token with flying."
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Denys Tsiperko"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21b63544-4c31-4f38-9907-0407719a60b1.jpg?1713091879"

        ruling(
            "2024-04-12",
            "A spell is modal if it has two or more options in a bulleted list and instructions for a " +
                "player to choose a number of those options, such as \"Choose one\" or \"Choose one or more.\" " +
                "Each of those options is a mode. Spells with spree are modal spells."
        )
        ruling(
            "2024-04-12",
            "The X in Riku's ability is the number of times you chose a mode for the modal spell you cast, " +
                "not the number of distinct modes you chose."
        )
        ruling(
            "2024-04-12",
            "You can't choose the same mode of Riku's triggered ability more than once each time it triggers."
        )
        ruling(
            "2024-04-12",
            "You pay all costs and follow all normal timing rules for cards played from exile with Riku's " +
                "triggered ability."
        )
    }
}
