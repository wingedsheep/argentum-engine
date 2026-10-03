package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glaring Fleshraker — Modern Horizons 3 #7 (uncommon)
 * {2}{C} · Creature — Eldrazi Drone · 2/2
 *
 * Whenever you cast a colorless spell, create a 0/1 colorless Eldrazi Spawn creature token with
 * "Sacrifice this token: Add {C}."
 * Whenever another colorless creature you control enters, this creature deals 1 damage to each
 * opponent.
 *
 * The cast trigger filters the spell on `CardPredicate.IsColorless`. The zone-change trigger matcher
 * has no color branches, so "another colorless creature you control enters" is the plain
 * `another(Creature.youControl()).enters()` subject narrowed by a `triggerRestriction` on the entering
 * creature — checked when the event happens and never again, which is the trigger condition's
 * timing (no intervening "if" to re-check on resolution). A Spawn made by the first trigger is
 * itself a colorless creature and fires the second.
 */
private val colorless = GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)

val GlaringFleshraker = card("Glaring Fleshraker") {
    manaCost = "{2}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a colorless spell, create a 0/1 colorless Eldrazi Spawn creature " +
        "token with \"Sacrifice this token: Add {C}.\"\n" +
        "Whenever another colorless creature you control enters, this creature deals 1 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.you.casts(colorless)
        effect = Effects.CreateEldraziSpawn()
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        triggerRestriction = Conditions.EntityMatches(EffectTarget.TriggeringEntity, colorless)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80c2a3c7-1486-4ff9-88ec-79ec67a437f8.jpg?1783911308"
    }
}
