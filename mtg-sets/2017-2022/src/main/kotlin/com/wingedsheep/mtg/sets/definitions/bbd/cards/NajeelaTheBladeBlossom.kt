package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Najeela, the Blade-Blossom (BBD #62)
 * {2}{R}
 * Legendary Creature — Human Warrior
 * 3/2
 * Whenever a Warrior attacks, you may have its controller create a 1/1 white Warrior creature
 * token that's tapped and attacking.
 * {W}{U}{B}{R}{G}: Untap all attacking creatures. They gain trample, lifelink, and haste until end
 * of turn. After this phase, there is an additional combat phase. Activate only during combat.
 *
 * The attack trigger watches every Warrior, not just yours: Najeela's controller makes the "may"
 * choice, and the token goes to the attacking Warrior's controller
 * ([Player.ControllerOfTriggeringEntity]). The token's defender is chosen by that controller as it
 * enters (CR 508.4) — [EnteringAttackDefender.CONTROLLER_CHOOSES] — so it needn't attack the same
 * player as the Warrior that triggered it. Tokens put onto the battlefield attacking were never
 * declared as attackers, so they don't retrigger the ability.
 *
 * The activated ability untaps and pumps every attacking creature (untapping doesn't remove them
 * from combat) and queues one extra combat phase directly after this one, with no main phase in
 * between ([Effects.AddCombatPhase] alone).
 */
val NajeelaTheBladeBlossom = card("Najeela, the Blade-Blossom") {
    manaCost = "{2}{R}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever a Warrior attacks, you may have its controller create a 1/1 white Warrior " +
        "creature token that's tapped and attacking.\n" +
        "{W}{U}{B}{R}{G}: Untap all attacking creatures. They gain trample, lifelink, and haste until " +
        "end of turn. After this phase, there is an additional combat phase. Activate only during combat."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR)).attacks()
        effect = Effects.May(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Warrior"),
                controller = EffectTarget.PlayerRef(Player.ControllerOfTriggeringEntity),
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                imageUri = "https://cards.scryfall.io/normal/front/d/3/d36e3c0e-a759-4adc-aa2f-61b13c0f786d.jpg?1783934653"
            )
        )
        description = "Whenever a Warrior attacks, you may have its controller create a 1/1 white Warrior " +
            "creature token that's tapped and attacking."
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.attacking()),
            Effects.Untap(EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        ) then Effects.AddCombatPhase
        restrictions = listOf(ActivationRestriction.DuringPhase(Phase.COMBAT))
        description = "{W}{U}{B}{R}{G}: Untap all attacking creatures. They gain trample, lifelink, and " +
            "haste until end of turn. After this phase, there is an additional combat phase. " +
            "Activate only during combat."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "62"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cb1d1da-6077-46b5-8c63-39882b8016f2.jpg?1783934856"

        ruling("2020-11-10", "Although the tokens created by Najeela's first ability are attacking, they were never declared as attacking creatures (for the purposes of abilities that trigger whenever a creature attacks, for example).")
        ruling("2020-11-10", "Untapping an attacking creature doesn't remove it from combat.")
        ruling("2020-11-10", "There is no main phase before the additional combat phase. This means that, for example, you can't activate an equip ability between combats.")
        ruling("2020-11-10", "The controller of each Warrior token created by Najeela chooses which player or planeswalker it's attacking. The tokens don't have to be attacking the same player or planeswalker as the creature that caused the ability to trigger. They can even be attacking players or planeswalkers that weren't being attacked.")
    }
}
