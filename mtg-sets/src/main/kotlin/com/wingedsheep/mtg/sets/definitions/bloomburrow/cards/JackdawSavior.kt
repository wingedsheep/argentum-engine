package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Jackdaw Savior
 * {2}{W}
 * Creature — Bird Cleric
 * 3/1
 *
 * Flying
 *
 * Whenever this creature or another creature you control with flying dies,
 * return another target creature card with lesser mana value from your
 * graveyard to the battlefield.
 *
 * The "lesser mana value" and "another" constraints are modeled using a pipeline:
 * Gather creature cards from graveyard → Exclude the triggering (dying) creature →
 * Filter by mana value less than the dying creature's → Select one → Move to
 * battlefield. This uses "choose" rather than "target" semantics, which is a minor
 * simplification.
 */
val JackdawSavior = card("Jackdaw Savior") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Bird Cleric"
    power = 3
    toughness = 1
    oracleText = "Flying\nWhenever this creature or another creature you control with flying dies, return another target creature card with lesser mana value from your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    // Trigger: whenever a creature you control with flying dies (includes self)
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.ANY
        )

        // Pipeline: gather creature cards from graveyard, exclude dying creature ("another"), filter by lesser MV, select one, move to battlefield
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                storeAs = "graveyardCreatures"
            ),
            FilterCollectionEffect(
                from = "graveyardCreatures",
                filter = CollectionFilter.ExcludeEntity(EntityReference.Triggering),
                storeMatching = "otherCreatures"
            ),
            FilterCollectionEffect(
                from = "otherCreatures",
                filter = CollectionFilter.ManaValueAtMost(
                    DynamicAmount.Subtract(
                        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.ManaValue),
                        DynamicAmount.Fixed(1)
                    )
                ),
                storeMatching = "validTargets"
            ),
            SelectFromCollectionEffect(
                from = "validTargets",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "chosen",
                selectedLabel = "Return to the battlefield"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            )
        ))

    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/121af600-6143-450a-9f87-12ce4833f1ec.jpg?1721425865"

        ruling("2024-07-26", "If Jackdaw Savior and another creature you control with flying die at the same time, Jackdaw Savior's last ability triggers for each of them.")
        ruling("2024-07-26", "The target creature card must have a lesser mana value than the creature that caused Jackdaw Savior's last ability to trigger. Use the mana value of that creature as it last existed on the battlefield to determine which creature cards in your graveyard are legal targets for that ability.")
    }
}
