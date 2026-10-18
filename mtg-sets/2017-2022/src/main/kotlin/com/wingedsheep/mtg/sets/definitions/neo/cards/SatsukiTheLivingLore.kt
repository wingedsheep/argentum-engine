package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Satsuki, the Living Lore
 * {G}{W}
 * Legendary Creature — Human Druid
 * 1/3
 *
 * {T}: Put a lore counter on each Saga you control. Activate only as a sorcery.
 * When Satsuki dies, choose up to one —
 * • Return target Saga or enchantment creature you control to its owner's hand.
 * • Return target Saga card from your graveyard to your hand.
 *
 * The tap ability puts one lore counter on each Saga you control via [Effects.ForEachInGroup];
 * each placement emits a `CountersAddedEvent`, which is what the engine's Saga chapter
 * detection reads — so the newly reached chapter triggers whenever the counter lands, not only
 * in the precombat main phase, and the final-chapter sacrifice follows as a state-based action
 * once that chapter has left the stack. The dies trigger is the "choose up to one" modal
 * (`chooseCount = 1, minChooseCount = 0`); both modes are targeted returns, so
 * [Effects.ReturnToHand] is the right facade (the graveyard target re-checks its zone on
 * resolution).
 */
val SatsukiTheLivingLore = card("Satsuki, the Living Lore") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Druid"
    power = 1
    toughness = 3
    oracleText = "{T}: Put a lore counter on each Saga you control. Activate only as a sorcery.\n" +
        "When Satsuki dies, choose up to one —\n" +
        "• Return target Saga or enchantment creature you control to its owner's hand.\n" +
        "• Return target Saga card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Enchantment.withSubtype("Saga").youControl()),
            Effects.AddCounters(CounterType.LORE, 1, EffectTarget.IterationEntity)
        )
        timing = TimingRule.SorcerySpeed
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Modal(
            modes = listOf(
                mode("Return target Saga or enchantment creature you control to its owner's hand") {
                    val permanent = target(
                        TargetFilter(
                            (
                                GameObjectFilter.Enchantment.withSubtype("Saga") or
                                    (GameObjectFilter.Enchantment and GameObjectFilter.Creature)
                                ).youControl()
                        )
                    )
                    effect = Effects.ReturnToHand(permanent)
                },
                mode("Return target Saga card from your graveyard to your hand") {
                    val sagaCard = target(
                        TargetFilter(
                            GameObjectFilter.Enchantment.withSubtype("Saga").ownedByYou(),
                            zone = Zone.GRAVEYARD
                        )
                    )
                    effect = Effects.ReturnToHand(sagaCard)
                }
            ),
            chooseCount = 1,
            minChooseCount = 0,
            countsAsModalSpell = false
        )
        description = "When Satsuki dies, choose up to one — Return target Saga or enchantment " +
            "creature you control to its owner's hand, or return target Saga card from your " +
            "graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0152575b-31b0-4142-9cc2-b87a81bd51a8.jpg?1783923830"
    }
}
