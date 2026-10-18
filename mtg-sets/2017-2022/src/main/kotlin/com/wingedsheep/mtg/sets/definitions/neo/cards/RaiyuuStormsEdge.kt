package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raiyuu, Storm's Edge — Kamigawa: Neon Dynasty #232 (canonical printing)
 * {2}{R}{W} · Legendary Creature — Human Samurai · 3/3
 *
 * First strike
 * Whenever a Samurai or Warrior you control attacks alone, untap it. If it's the first combat phase
 * of the turn, there is an additional combat phase after this phase.
 *
 * The attacks-alone shape of Eiganjo Exemplar / Asari Captain; "it" is the lone attacker
 * ([EffectTarget.TriggeringEntity]). The untap is unconditional; the first-combat check is a
 * resolution-time "if" (not an intervening-if), so the extra combat it adds can untap its attacker
 * again but never spawns a third combat. Combat only — no extra main phase.
 */
private val samuraiOrWarrior =
    GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR))

val RaiyuuStormsEdge = card("Raiyuu, Storm's Edge") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 3
    oracleText = "First strike\nWhenever a Samurai or Warrior you control attacks alone, untap it. " +
        "If it's the first combat phase of the turn, there is an additional combat phase after this phase."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.a(samuraiOrWarrior.youControl()).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.Untap(EffectTarget.TriggeringEntity) then
            Effects.If(Conditions.IsFirstCombatPhaseOfTurn, Effects.AddCombatPhase)
        description = "Whenever a Samurai or Warrior you control attacks alone, untap it. If it's the " +
            "first combat phase of the turn, there is an additional combat phase after this phase."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Heonhwa"
        flavorText = "\"The tempest is the only ally I need.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37748b74-0eda-4b20-a08f-faa418adb9ff.jpg?1783923831"
        ruling(
            "2022-02-18",
            "A creature attacks alone if it's the only creature declared as an attacker during the declare " +
                "attackers step. For example, Raiyuu's triggered ability won't trigger if you attack with " +
                "multiple creatures and all but one of them are removed from combat."
        )
        ruling(
            "2022-02-18",
            "Notably, the triggered ability doesn't give you any additional main phases. This means that " +
                "you will move directly from the end of combat step of one combat phase to the beginning " +
                "of combat step of the next one."
        )
    }
}
