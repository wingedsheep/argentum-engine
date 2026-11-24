package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archon of the Triumvirate
 * {5}{W}{U}
 * Creature — Archon
 * 4/5
 * Flying
 * Whenever this creature attacks, detain up to two target nonland permanents your opponents
 * control. (Until your next turn, those permanents can't attack or block and their activated
 * abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Detain is spelled out per target, the same until-your-next-turn lock Lyev Decree applies: can't
 * attack or block, plus a granted activation ban.
 */
val ArchonOfTheTriumvirate = card("Archon of the Triumvirate") {
    manaCost = "{5}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Archon"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever this creature attacks, detain up to two target nonland permanents your opponents " +
        "control. (Until your next turn, those permanents can't attack or block and their activated " +
        "abilities can't be activated.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        targets(TargetFilter.NonlandPermanentOpponentControls, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.CantAttackOrBlock(EffectTarget.ContextTarget(0), Duration.UntilYourNextTurn) then
                Effects.GrantStaticAbility(
                    ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                    target = EffectTarget.ContextTarget(0),
                    duration = Duration.UntilYourNextTurn,
                )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf91d847-4a87-4a65-8d6d-e20d538c5cec.jpg?1783940345"
        ruling("2012-10-01", "The nonland permanents will be detained before blockers are chosen. Notably, a creature detained this way can't block before it is detained.")
        ruling("2012-10-01", "The two nonland permanents can be controlled by the same opponent or by different opponents.")
        ruling(
            "2013-04-15",
            "Activated abilities include a colon and are written in the form \"[cost]: [effect].\" No one can activate any activated abilities, including mana abilities, of a detained permanent."
        )
        ruling("2013-04-15", "The static abilities of a detained permanent still apply. The triggered abilities of a detained permanent can still trigger.")
        ruling("2013-04-15", "If a creature is already attacking or blocking when it's detained, it won't be removed from combat. It will continue to attack or block.")
        ruling("2013-04-15", "If a permanent's activated ability is on the stack when that permanent is detained, the ability will be unaffected.")
        ruling("2013-04-15", "If a noncreature permanent is detained and later turns into a creature, it won't be able to attack or block.")
    }
}
