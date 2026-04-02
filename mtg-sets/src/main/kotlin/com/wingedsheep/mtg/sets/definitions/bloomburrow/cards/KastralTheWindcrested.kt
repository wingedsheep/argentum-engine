package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kastral, the Windcrested
 * {3}{W}{U}
 * Legendary Creature — Bird Scout
 * 4/5
 *
 * Flying
 * Whenever one or more Birds you control deal combat damage to a player, choose one —
 * • You may put a Bird creature card from your hand or graveyard onto the battlefield
 *   with a finality counter on it.
 * • Put a +1/+1 counter on each Bird you control.
 * • Draw a card.
 */
val KastralTheWindcrested = card("Kastral, the Windcrested") {
    manaCost = "{3}{W}{U}"
    typeLine = "Legendary Creature — Bird Scout"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever one or more Birds you control deal combat damage to a player, choose one —\n" +
        "• You may put a Bird creature card from your hand or graveyard onto the battlefield " +
        "with a finality counter on it.\n" +
        "• Put a +1/+1 counter on each Bird you control.\n" +
        "• Draw a card."

    keywords(Keyword.FLYING)

    // Whenever one or more Birds you control deal combat damage to a player, choose one —
    triggeredAbility {
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.withSubtype("Bird")
            ),
            TriggerBinding.ANY
        )
        effect = ModalEffect.chooseOne(
            // Mode 1: Put a Bird creature card from your hand or graveyard onto the battlefield
            // with a finality counter on it (optional — "you may")
            Mode.noTarget(
                CompositeEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromMultipleZones(
                                zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                                player = Player.You,
                                filter = GameObjectFilter.Creature.withSubtype("Bird")
                            ),
                            storeAs = "birds"
                        ),
                        SelectFromCollectionEffect(
                            from = "birds",
                            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                            storeSelected = "chosen",
                            prompt = "Choose a Bird creature card to put onto the battlefield"
                        ),
                        MoveCollectionEffect(
                            from = "chosen",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                        ),
                        AddCountersToCollectionEffect("chosen", Counters.FINALITY, 1)
                    )
                ),
                "Put a Bird creature card from your hand or graveyard onto the battlefield with a finality counter on it"
            ),
            // Mode 2: Put a +1/+1 counter on each Bird you control
            Mode.noTarget(
                ForEachInGroupEffect(
                    filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Bird").youControl()),
                    effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                ),
                "Put a +1/+1 counter on each Bird you control"
            ),
            // Mode 3: Draw a card
            Mode.noTarget(
                DrawCardsEffect(count = DynamicAmount.Fixed(1), target = EffectTarget.Controller),
                "Draw a card"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebf68793-22a2-4a59-9d37-6791584edca1.jpg?1721427101"

        ruling("2024-07-26", "Finality counters work on any permanent, not only creatures. If a permanent with a finality counter on it would be put into a graveyard from the battlefield, exile it instead.")
        ruling("2024-07-26", "Finality counters don't stop permanents from going to zones other than the graveyard from the battlefield.")
        ruling("2024-07-26", "Finality counters aren't keyword counters, and a finality counter doesn't give any abilities to the permanent it's on.")
        ruling("2024-07-26", "Multiple finality counters on a single permanent are redundant.")
    }
}
