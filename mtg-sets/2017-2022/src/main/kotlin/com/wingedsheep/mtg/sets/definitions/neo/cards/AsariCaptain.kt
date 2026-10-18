package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Asari Captain — Kamigawa: Neon Dynasty #215 (canonical printing)
 * {3}{R}{W} · Creature — Human Samurai · 4/3
 *
 * Haste
 * Whenever a Samurai or Warrior you control attacks alone, it gets +1/+0 until end of turn for each
 * Samurai or Warrior you control.
 *
 * The attacks-alone shape of Eiganjo Exemplar; "it" is the lone attacker ([EffectTarget.TriggeringEntity]).
 * The count is taken on resolution over creatures matching *either* subtype, so a Samurai Warrior is
 * counted once and the attacker counts itself.
 */
private val samuraiOrWarrior =
    GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR))

val AsariCaptain = card("Asari Captain") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Samurai"
    power = 4
    toughness = 3
    oracleText = "Haste\nWhenever a Samurai or Warrior you control attacks alone, it gets +1/+0 until " +
        "end of turn for each Samurai or Warrior you control."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(samuraiOrWarrior.youControl()).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.ModifyStats(
            DynamicAmounts.battlefield(Player.You, samuraiOrWarrior).count(),
            DynamicAmounts.fixed(0),
            EffectTarget.TriggeringEntity,
        )
        description = "Whenever a Samurai or Warrior you control attacks alone, it gets +1/+0 until end " +
            "of turn for each Samurai or Warrior you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Donato Giancola"
        flavorText = "\"Go! Show them your fury!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/834b4e6a-1812-4399-8fe4-bb1686ba49ef.jpg?1783923838"
        ruling(
            "2022-02-18",
            "Asari Captain's triggered ability gives the solitary attacker +1/+0 for each creature you " +
                "control that is either a Samurai or a Warrior as it resolves, including the attacker " +
                "itself, if applicable. A creature that is both a Samurai and a Warrior is only counted once."
        )
    }
}
