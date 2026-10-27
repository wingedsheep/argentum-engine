package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Karlach, Fury of Avernus
 * {4}{R}
 * Legendary Creature — Tiefling Barbarian
 * 5/4
 *
 * Whenever you attack, if it's the first combat phase of the turn, untap all attacking creatures.
 * They gain first strike until end of turn. After this phase, there is an additional combat phase.
 * Choose a Background
 *
 * "Whenever you attack" is `Triggers.you.attacks()` — Karlach needn't be among the attackers. The
 * intervening "if it's the first combat phase of the turn" is Balthier and Fran's
 * [Conditions.IsFirstCombatPhaseOfTurn] guard, so attacking in the extra combat doesn't loop. The
 * resolution is Najeela, the Blade-Blossom's untap-and-grant over every attacking creature followed
 * by [Effects.AddCombatPhase] (no extra main phase, per the ruling).
 */
val KarlachFuryOfAvernus = card("Karlach, Fury of Avernus") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Tiefling Barbarian"
    power = 5
    toughness = 4
    oracleText = "Whenever you attack, if it's the first combat phase of the turn, untap all attacking " +
        "creatures. They gain first strike until end of turn. After this phase, there is an additional " +
        "combat phase.\n" +
        "Choose a Background (You can have a Background as a second commander.)"

    triggeredAbility {
        trigger = Triggers.you.attacks()
        interveningIf = Conditions.IsFirstCombatPhaseOfTurn
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.attacking()),
            Effects.Untap(EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.IterationEntity)
        ) then Effects.AddCombatPhase
        description = "Whenever you attack, if it's the first combat phase of the turn, untap all attacking " +
            "creatures. They gain first strike until end of turn. After this phase, there is an additional " +
            "combat phase."
    }

    keywordAbility(KeywordAbility.chooseABackground())

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/775480ed-f574-40cb-bd9b-21102eaaff63.jpg?1783922734"
        ruling(
            "2022-06-10",
            "Notably, the triggered ability of Karlach doesn't give you any additional main phases. This means " +
                "that you will move directly from the end of combat step of one combat phase to the beginning of " +
                "combat step of the next one."
        )
        ruling("2022-06-10", "Karlach doesn't have to be among the attacking creatures.")
    }
}
