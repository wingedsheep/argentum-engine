package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evereth, Viceroy of Plunder — Foundations Jumpstart #41
 * {2}{B} · Legendary Creature — Vampire Soldier · 2/2
 *
 * Flying
 * Sacrifice another creature or artifact: Put a +1/+1 counter on Evereth. If the sacrificed
 * permanent was a Treasure, Evereth gains lifelink until end of turn. Activate only as a sorcery.
 * When Evereth dies, you may pay {1}{B/R}. When you do, Evereth deals damage equal to its power to
 * each opponent.
 *
 * The Treasure rider reads the cost-sacrificed permanent's snapshot (SacrificedHadSubtype). The
 * dies trigger is a reflexive trigger over an optional mana payment; "its power" is Evereth's
 * last-known power (ruling 2024-11-08), which the source-power read takes from the battlefield-exit
 * snapshot once Evereth is in the graveyard.
 */
val EverethViceroyOfPlunder = card("Evereth, Viceroy of Plunder") {
    manaCost = "{2}{B}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Sacrifice another creature or artifact: Put a +1/+1 counter on Evereth. If the sacrificed " +
        "permanent was a Treasure, Evereth gains lifelink until end of turn. Activate only as a sorcery.\n" +
        "When Evereth dies, you may pay {1}{B/R}. When you do, Evereth deals damage equal to its power " +
        "to each opponent."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature or GameObjectFilter.Artifact)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SacrificedHadSubtype(Subtype.TREASURE.value),
                then = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
            )
        description = "Sacrifice another creature or artifact: Put a +1/+1 counter on Evereth. If the " +
            "sacrificed permanent was a Treasure, Evereth gains lifelink until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}{B/R}"),
            reflexiveEffect = Effects.DealDamage(
                DynamicAmounts.sourcePower(),
                EffectTarget.PlayerRef(Player.EachOpponent)
            ),
            descriptionOverride = "You may pay {1}{B/R}. When you do, Evereth deals damage equal to its " +
                "power to each opponent."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Yuichi Murakami"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9c9c0be-8c69-45e5-bdcb-e62ddb05e532.jpg?1783908858"
        ruling(
            "2024-11-08",
            "Use Evereth's power as it last existed on the battlefield to determine how much damage it " +
                "deals with its last ability."
        )
    }
}
