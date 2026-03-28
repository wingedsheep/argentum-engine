package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.AttackEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Zoraline, Cosmos Caller
 * {1}{W}{B}
 * Legendary Creature — Bat Cleric
 * 3/3
 *
 * Flying, vigilance
 * Whenever a Bat you control attacks, you gain 1 life.
 * Whenever Zoraline enters or attacks, you may pay {W}{B} and 2 life.
 * When you do, return target nonland permanent card with mana value 3
 * or less from your graveyard to the battlefield with a finality counter on it.
 */
val ZoralineCosmosCaller = card("Zoraline, Cosmos Caller") {
    manaCost = "{1}{W}{B}"
    typeLine = "Legendary Creature — Bat Cleric"
    power = 3
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Whenever a Bat you control attacks, you gain 1 life.\n" +
        "Whenever Zoraline enters or attacks, you may pay {W}{B} and 2 life. " +
        "When you do, return target nonland permanent card with mana value 3 or less " +
        "from your graveyard to the battlefield with a finality counter on it."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    // Whenever a Bat you control attacks, you gain 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            AttackEvent(filter = GameObjectFilter.Creature.withSubtype("Bat").youControl()),
            TriggerBinding.ANY
        )
        effect = Effects.GainLife(1)
    }

    // Whenever Zoraline enters the battlefield, you may pay {W}{B} and 2 life.
    // When you do, return target nonland permanent card with MV ≤ 3 from your graveyard
    // to the battlefield with a finality counter on it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = zoralineReanimateEffect()
    }

    // Whenever Zoraline attacks, same effect.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = zoralineReanimateEffect()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f99fd5-5298-4b27-923d-9d31203c931a.jpg?1721427252"

        ruling("2024-07-26", "You don't choose a target for Zoraline's last ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you pay {W}{B} and 2 life this way. You choose a target for this ability as it goes on the stack.")
        ruling("2024-07-26", "If a permanent with a finality counter on it would be put into a graveyard from the battlefield, exile it instead.")
        ruling("2024-07-26", "Finality counters don't stop permanents from going to zones other than the graveyard from the battlefield.")
        ruling("2024-07-26", "Multiple finality counters on a single permanent are redundant.")
    }
}

/**
 * Creates the effect for Zoraline's reflexive trigger:
 * May pay {W}{B} and 2 life → return nonland permanent card MV ≤ 3
 * from graveyard to battlefield with a finality counter.
 */
private fun zoralineReanimateEffect() = OptionalCostEffect(
    cost = CompositeEffect(
        listOf(
            PayManaCostEffect(ManaCost.parse("{W}{B}")),
            PayLifeEffect(2)
        )
    ),
    ifPaid = CompositeEffect(
        listOf(
            // Gather nonland permanent cards with MV ≤ 3 from your graveyard
            GatherCardsEffect(
                source = CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.NonlandPermanent.manaValueAtMost(3)
                ),
                storeAs = "eligible"
            ),
            // Select one to return
            SelectFromCollectionEffect(
                from = "eligible",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "chosen",
                prompt = "Choose a nonland permanent card with mana value 3 or less to return to the battlefield"
            ),
            // Move to battlefield
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            ),
            // Add finality counter
            AddCountersToCollectionEffect("chosen", "finality", 1)
        )
    )
)
