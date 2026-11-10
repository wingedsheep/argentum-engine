package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eldrazi Mimic
 * {2}
 * Creature — Eldrazi
 * 2/1
 * Whenever another colorless creature you control enters, you may have this creature's base power
 * and toughness become equal to that creature's power and toughness until end of turn.
 *
 * The "another colorless creature you control enters" subject is the Glaring Fleshraker shape: the
 * zone-change matcher has no color branch, so `another(Creature.youControl()).enters()` is narrowed
 * by a `triggerRestriction` on the entering creature. The copy is a layer-7b set-values floating
 * effect ([Effects.SetBasePowerAndToughness]) whose amounts are snapshotted at resolution from the
 * triggering creature's power and toughness — live while it is on the battlefield, its last-known
 * values once it has left (ruling 2016-01-22, possibly negative).
 */
private val colorless = GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)

val EldraziMimic = card("Eldrazi Mimic") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 2
    toughness = 1
    oracleText = "Whenever another colorless creature you control enters, you may have this creature's " +
        "base power and toughness become equal to that creature's power and toughness until end of turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        triggerRestriction = Conditions.EntityMatches(EffectTarget.TriggeringEntity, colorless)
        optional = true
        effect = Effects.SetBasePowerAndToughness(
            power = DynamicAmounts.triggeringPower(),
            toughness = DynamicAmounts.triggeringToughness(),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Craig J Spearing"
        flavorText = "It is consistently inconsistent."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/311a05b1-042d-47e7-9fd7-6e8abe8fc578.jpg?1783937930"
        ruling(
            "2016-01-22",
            "Use the new creature's power and toughness at the time the ability resolves to determine the base power and toughness of Eldrazi Mimic. If that creature is no longer on the battlefield at that time, use its power and toughness when it left the battlefield. (Keep in mind that those values may be negative, for example if a spell like Spatial Contortion giving it +3/-3 is what caused it to leave the battlefield.)"
        )
        ruling(
            "2016-01-22",
            "Eldrazi Mimic's ability will overwrite any other effect setting its base power and toughness. Such effects that begin to apply after that ability resolves will similarly overwrite the ability. Effects that modify Eldrazi Mimic's power and/or toughness but don't set its base power and/or toughness to specific values will apply no matter when they started to apply. The same is true for any +1/+1 counters it may have."
        )
    }
}
