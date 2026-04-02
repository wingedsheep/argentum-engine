package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oakhollow Village
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {G}. Spend this mana only to cast a creature spell.
 * {G}, {T}: Put a +1/+1 counter on each Frog, Rabbit, Raccoon, or Squirrel you control
 * that entered the battlefield this turn.
 */
val OakhollowVillage = card("Oakhollow Village") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {G}. Spend this mana only to cast a creature spell.\n" +
        "{G}, {T}: Put a +1/+1 counter on each Frog, Rabbit, Raccoon, or Squirrel you control " +
        "that entered the battlefield this turn."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN, restriction = ManaRestriction.CreatureSpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(
                    Zone.BATTLEFIELD,
                    Player.You,
                    GameObjectFilter.Creature
                        .withAnyOfSubtypes(
                            listOf(
                                Subtype("Frog"),
                                Subtype("Rabbit"),
                                Subtype("Raccoon"),
                                Subtype("Squirrel")
                            )
                        )
                        .enteredThisTurn()
                ),
                storeAs = "creatures"
            ),
            AddCountersToCollectionEffect("creatures", Counters.PLUS_ONE_PLUS_ONE, 1)
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "258"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d49b016-b02b-459f-85e9-c04f6bdcb94e.jpg?1721639587"
    }
}
