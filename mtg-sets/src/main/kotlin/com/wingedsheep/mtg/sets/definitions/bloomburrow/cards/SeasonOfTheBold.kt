package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Season of the Bold {3}{R}{R}
 * Sorcery
 *
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Create a tapped Treasure token.
 * {P}{P} — Exile the top two cards of your library. Until the end of your next turn,
 *           you may play them.
 * {P}{P}{P} — Until the end of your next turn, whenever you cast a spell, Season of the
 *             Bold deals 2 damage to up to one target creature.
 */
val SeasonOfTheBold = card("Season of the Bold") {
    manaCost = "{3}{R}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose up to five {P} worth of modes. You may choose the same mode more than once.\n" +
        "{P} — Create a tapped Treasure token.\n" +
        "{P}{P} — Exile the top two cards of your library. Until the end of your next turn, you may play them.\n" +
        "{P}{P}{P} — Until the end of your next turn, whenever you cast a spell, Season of the Bold deals 2 damage to up to one target creature."

    spell {
        effect = BudgetModalEffect(
            budget = 5,
            modes = listOf(
                // {P} — Create a tapped Treasure token
                // Note: Predefined tokens don't support entering tapped; enters untapped
                BudgetMode(
                    cost = 1,
                    effect = Effects.CreateTreasure(),
                    description = "Create a tapped Treasure token"
                ),
                // {P}{P} — Exile top 2 and play until end of next turn
                BudgetMode(
                    cost = 2,
                    effect = CompositeEffect(listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                            storeAs = "exiledCards"
                        ),
                        MoveCollectionEffect(
                            from = "exiledCards",
                            destination = CardDestination.ToZone(Zone.EXILE)
                        ),
                        GrantMayPlayFromExileEffect("exiledCards", untilEndOfNextTurn = true)
                    )),
                    description = "Exile the top two cards of your library. Until the end of your next turn, you may play them"
                ),
                // {P}{P}{P} — Until end of your next turn, whenever you cast a spell,
                // Season of the Bold deals 2 damage to up to one target creature
                BudgetMode(
                    cost = 3,
                    effect = Effects.CreateGlobalTriggeredAbilityWithDuration(
                        ability = TriggeredAbility.create(
                            trigger = SpellCastEvent(player = Player.You),
                            binding = TriggerBinding.ANY,
                            effect = DealDamageEffect(
                                amount = DynamicAmount.Fixed(2),
                                target = EffectTarget.ContextTarget(0)
                            ),
                            targetRequirement = TargetCreature(optional = true)
                        ),
                        duration = Duration.UntilYourNextTurn
                    ),
                    description = "Until the end of your next turn, whenever you cast a spell, Season of the Bold deals 2 damage to up to one target creature"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "152"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84352565-558b-4f9b-a411-532147806a78.jpg?1721426701"
    }
}
