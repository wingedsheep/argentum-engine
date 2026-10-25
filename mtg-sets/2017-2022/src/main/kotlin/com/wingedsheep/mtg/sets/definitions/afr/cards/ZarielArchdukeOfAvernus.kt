package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zariel, Archduke of Avernus — Adventures in the Forgotten Realms #172 (canonical printing)
 * {2}{R}{R} · Legendary Planeswalker — Zariel · Loyalty 4
 *
 * +1: Creatures you control get +1/+0 and gain haste until end of turn.
 * 0: Create a 1/1 red Devil creature token with "When this token dies, it deals 1 damage to any
 *    target."
 * −6: You get an emblem with "At the end of the first combat phase on your turn, untap target
 *    creature you control. After this phase, there is an additional combat phase."
 *
 * The +1 is one [Patterns.Group.pumpAndGrantToAll] pass (Doomskar Titan's shape).
 *
 * The emblem is a permanent [Effects.CreateGlobalTriggeredAbility] (Teferi, Hero of Dominaria's
 * targeted-emblem shape). "At the end of the first combat phase on your turn" is the end-of-combat
 * step trigger on your turn (CR 511.2: "at end of combat" abilities trigger as that step begins),
 * narrowed by a trigger-time [Conditions.IsFirstCombatPhaseOfTurn] restriction — the end of combat
 * step still belongs to the first combat phase, and the "a combat phase ended" marker is only set
 * as that step is left, so the restriction is true here and false at the end of every later combat.
 * The additional combat phase is part of the resolving ability, so an illegal target fizzles the
 * whole thing and no combat is added (ruling). Several emblems each trigger and each queue a combat
 * (ruling); no main phase is added between combats.
 */
val ZarielArchdukeOfAvernus = card("Zariel, Archduke of Avernus") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Zariel"
    startingLoyalty = 4
    oracleText = "+1: Creatures you control get +1/+0 and gain haste until end of turn.\n" +
        "0: Create a 1/1 red Devil creature token with \"When this token dies, it deals 1 damage to " +
        "any target.\"\n" +
        "−6: You get an emblem with \"At the end of the first combat phase on your turn, untap target " +
        "creature you control. After this phase, there is an additional combat phase.\""

    // +1: Creatures you control get +1/+0 and gain haste until end of turn.
    loyaltyAbility(+1) {
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 0,
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    // 0: Create a 1/1 red Devil creature token with "When this token dies, it deals 1 damage to any target."
    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    val any = target(Targets.Any)
                    effect = Effects.DealDamage(1, any)
                    description = "When this token dies, it deals 1 damage to any target."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/e/f/efbc2ca1-4aad-4dfa-9eab-e783f5802078.jpg?1783926343",
        )
    }

    // −6: You get an emblem with "At the end of the first combat phase on your turn, untap target
    //     creature you control. After this phase, there is an additional combat phase."
    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END_COMBAT)
                triggerRestriction = Conditions.IsFirstCombatPhaseOfTurn
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.Untap(creature) then Effects.AddCombatPhase
                description = "At the end of the first combat phase on your turn, untap target " +
                    "creature you control. After this phase, there is an additional combat phase."
            },
            descriptionOverride = "At the end of the first combat phase on your turn, untap target " +
                "creature you control. After this phase, there is an additional combat phase."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "172"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7be5b866-d479-4698-98b2-87a973f6b8f6.jpg?1783926467"
        ruling(
            "2021-07-23",
            "There is no main phase between the combat phases. You move from the first combat phase " +
                "directly into the second combat phase."
        )
        ruling(
            "2021-07-23",
            "If the target of the emblem's ability becomes an illegal target by the time the ability " +
                "tries to resolve, the ability won't resolve and none of its effects will happen. You " +
                "won't get an additional combat phase."
        )
        ruling(
            "2021-07-23",
            "If you have more than one of Zariel's emblem, the abilities will each trigger at the end " +
                "of the first combat phase on your turn. You can choose the same creature as the target " +
                "of each ability. Those targets will untap before any additional combats."
        )
    }
}
