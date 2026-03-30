package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Clement, the Worrywort
 * {1}{G}{U}
 * Legendary Creature — Frog Druid
 * 3/3
 *
 * Vigilance
 * Whenever Clement or another creature you control enters, return up to one
 * target creature you control with lesser mana value to its owner's hand.
 * Frogs you control have "{T}: Add {G} or {U}. Spend this mana only to cast
 * a creature spell."
 */
val ClementTheWorrywort = card("Clement, the Worrywort") {
    manaCost = "{1}{G}{U}"
    typeLine = "Legendary Creature — Frog Druid"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever Clement or another creature you control enters, return up to one target creature " +
        "you control with lesser mana value to its owner's hand.\n" +
        "Frogs you control have \"{T}: Add {G} or {U}. Spend this mana only to cast a creature spell.\""

    keywords(Keyword.VIGILANCE)

    // Whenever Clement or another creature you control enters, return up to one
    // creature you control with lesser MV to hand.
    // Uses a pipeline: gather creatures you control, filter by MV < triggering entity's MV,
    // select up to one, return to hand.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = CompositeEffect(listOf(
            // Gather all creatures you control on the battlefield
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.BATTLEFIELD, Player.You, GameObjectFilter.Creature),
                storeAs = "myCreatures"
            ),
            // Filter to those with MV strictly less than the entering creature's MV
            FilterCollectionEffect(
                from = "myCreatures",
                filter = CollectionFilter.ManaValueAtMost(
                    DynamicAmount.Subtract(
                        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.ManaValue),
                        DynamicAmount.Fixed(1)
                    )
                ),
                storeMatching = "eligible"
            ),
            // Select up to one to return to hand
            SelectFromCollectionEffect(
                from = "eligible",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "chosen",
                selectedLabel = "Return to hand"
            ),
            // Move the chosen creature to its owner's hand
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        ))
    }

    // Frogs you control have "{T}: Add {G} or {U}. Spend this mana only to cast a creature spell."
    staticAbility {
        ability = GrantActivatedAbilityToCreatureGroup(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = AddDynamicManaEffect(
                    amountSource = DynamicAmount.Fixed(1),
                    allowedColors = setOf(Color.GREEN, Color.BLUE),
                    restriction = ManaRestriction.CreatureSpellsOnly
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add {G} or {U}. Spend this mana only to cast a creature spell."
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Frog").youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7028130c-c91d-4bf7-b0b0-450f71107d7a.jpg?1721427029"
    }
}
