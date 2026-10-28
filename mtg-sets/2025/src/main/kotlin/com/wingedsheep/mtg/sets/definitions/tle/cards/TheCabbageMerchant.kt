package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Cabbage Merchant — Avatar: The Last Airbender Eternal #134 (canonical / earliest real printing, 2025)
 * {2}{G} · Legendary Creature — Human Citizen · 2/2
 *
 * Whenever an opponent casts a noncreature spell, create a Food token.
 * Whenever a creature deals combat damage to you, sacrifice a Food token.
 * Tap two untapped Foods you control: Add one mana of any color.
 *
 * - Food trigger: `Triggers.anOpponent.casts(Noncreature)` (Kambal's trigger) → `CreateFood()`.
 * - Combat-damage trigger: `Triggers.a(Creature).dealsCombatDamage(Recipient.You)` (Strixhaven
 *   Stadium's trigger, once per creature) → a forced sacrifice of one Food *token* you control. A
 *   sacrificed Food's own life-gain ability doesn't run (ruling).
 * - Mana ability: Birchlore Rangers' shape — `TapPermanents(2, Food)` as the whole cost. "Foods"
 *   means any Food artifact, token or not (ruling).
 */
val TheCabbageMerchant = card("The Cabbage Merchant") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "Whenever an opponent casts a noncreature spell, create a Food token. (It's an " +
        "artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever a creature deals combat damage to you, sacrifice a Food token.\n" +
        "Tap two untapped Foods you control: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateFood()
        description = "Whenever an opponent casts a noncreature spell, create a Food token."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.You)
        effect = Effects.Sacrifice(
            GameObjectFilter.Artifact.withSubtype("Food").token(),
            1,
            EffectTarget.Controller,
        )
        description = "Whenever a creature deals combat damage to you, sacrifice a Food token."
    }

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Artifact.withSubtype("Food"))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Add one mana of any color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Patrick Gañas"
        flavorText = "\"My cabbages...\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fea0356-6684-4730-9eb4-0262856bc1f9.jpg?1783904816"
        ruling(
            "2025-10-02",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token. You " +
                "can't sacrifice a Food to pay multiple costs."
        )
        ruling(
            "2025-10-02",
            "Food sacrificed due to The Cabbage Merchant's second triggered ability is not being sacrificed " +
                "to the Food's own ability, so you will not gain any life."
        )
    }
}
