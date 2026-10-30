package com.wingedsheep.mtg.sets.definitions.acr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sigurd, Jarl of Ravensthorpe — Assassin's Creed #66 (canonical printing)
 * {R}{G}{W} · Legendary Creature — Human Warrior · 3/3
 *
 * Vigilance, trample, lifelink
 * Boast — {1}: Put a lore counter on target Saga you control or remove one from it.
 * Whenever you put a lore counter on a Saga you control, put a +1/+1 counter on up to one other
 * target creature.
 *
 * The boast's "or" is a resolution-time choice ([Effects.ChooseAction]); with no lore counter on
 * the Saga only "put" is offered. The trigger sees every lore counter you put — the boast, Saga
 * entry counters and the precombat-main turn-based action (CR 714.3c) all carry their placer.
 */
val SigurdJarlOfRavensthorpe = card("Sigurd, Jarl of Ravensthorpe") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Vigilance, trample, lifelink\n" +
        "Boast — {1}: Put a lore counter on target Saga you control or remove one from it. " +
        "(Activate only if this creature attacked this turn and only once each turn.)\n" +
        "Whenever you put a lore counter on a Saga you control, put a +1/+1 counter on up to one " +
        "other target creature."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.LIFELINK)

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}")
        val saga = target(TargetFilter(GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA).youControl()))
        val addCounter = Effects.AddCounters(CounterType.LORE, 1, saga)
        effect = Effects.If(
            condition = Conditions.TargetHasCounter(CounterType.LORE, saga),
            then = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice("Put a lore counter on the Saga", addCounter),
                    EffectChoice("Remove a lore counter from the Saga", Effects.RemoveCounters(CounterType.LORE, 1, saga)),
                ),
            ),
            otherwise = addCounter,
        )
        description = "Put a lore counter on target Saga you control or remove one from it"
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA).youControl())
            .getsCounters(CounterType.LORE, by = Player.You)
        val creature = target(TargetFilter.Creature.other(), optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Alexander Gering"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25ceade9-f12a-4724-b198-312fb4b1210b.jpg?1783910967"
        ruling("2024-07-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2024-07-05", "If it’s not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature’s boast ability if it hasn’t been activated yet that turn.")
        ruling("2024-07-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can’t be activated that turn.")
        ruling("2024-07-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
        ruling("2024-07-05", "Putting a lore counter on a Saga will usually cause the next chapter ability to trigger. If it already has a number of lore counters on it equal to its final chapter number however, no abilities trigger. In particular, this doesn’t cause the final chapter ability to trigger again.")
    }
}
