package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDraw
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mornsong Aria
 * {1}{B}{B}
 * Legendary Enchantment
 *
 * Players can't draw cards or gain life.
 * At the beginning of each player's draw step, that player loses 3 life,
 * searches their library for a card, puts it into their hand, then shuffles.
 */
val MornsongAria = card("Mornsong Aria") {
    manaCost = "{1}{B}{B}"
    typeLine = "Legendary Enchantment"
    oracleText = "Players can't draw cards or gain life.\n" +
        "At the beginning of each player's draw step, that player loses 3 life, searches their library for a card, puts it into their hand, then shuffles."

    replacementEffect(PreventDraw(appliesTo = GameEvent.DrawEvent(player = Player.Each)))
    replacementEffect(PreventLifeGain(appliesTo = GameEvent.LifeGainEvent(player = Player.Each)))

    triggeredAbility {
        trigger = TriggerSpec(
            event = GameEvent.StepEvent(Step.DRAW, Player.Each),
            binding = TriggerBinding.ANY
        )
        effect = Effects.LoseLife(3, target = EffectTarget.PlayerRef(Player.TriggeringPlayer))
            .then(
                CompositeEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(Zone.LIBRARY, Player.TriggeringPlayer, GameObjectFilter.Any),
                            storeAs = "searchable"
                        ),
                        SelectFromCollectionEffect(
                            from = "searchable",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            chooser = Chooser.TriggeringPlayer,
                            storeSelected = "found",
                            prompt = "Search your library for a card",
                            selectedLabel = "Put into hand"
                        ),
                        MoveCollectionEffect(
                            from = "found",
                            destination = CardDestination.ToZone(Zone.HAND, Player.TriggeringPlayer)
                        ),
                        ShuffleLibraryEffect(target = EffectTarget.PlayerRef(Player.TriggeringPlayer))
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Scott M. Fischer"
        flavorText = "Maralen is more forgiving than Oona was, but her rule is no less absolute."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9985c554-8338-46b1-ac36-526d2eb61570.jpg?1767658159"
        ruling("2025-11-17", "If an effect says to set a player's life total to a number that's higher than the player's current life total while Mornsong Aria is on the battlefield, the player's life total doesn't change.")
        ruling("2025-11-17", "Spells and abilities that cause players to draw cards or gain life still resolve while Mornsong Aria is on the battlefield. No player will draw cards or gain life, but any other effects of that spell or ability will still happen.")
    }
}
