package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.IsInStep
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Zurgo, Thunder's Decree — Tarkir: Dragonstorm #237
 * {R}{W}{B} · Legendary Creature — Orc Warrior · Rare
 * 2/4
 *
 * Mobilize 2 (Whenever this creature attacks, create two tapped and attacking 1/1 red Warrior
 * creature tokens. Sacrifice them at the beginning of the next end step.)
 * During your end step, Warrior tokens you control have "This token can't be sacrificed."
 *
 * The mobilize tokens carry a delayed "sacrifice at the next end step" trigger. Zurgo's static
 * gives Warrior tokens you control "can't be sacrificed" during your end step, so that delayed
 * sacrifice no-ops and the tokens stick around. Modeled with the time-restricted
 * [ConditionalStaticAbility] gating a [CantBeSacrificed] static via [IsInStep] (your end step).
 */
val ZurgoThundersDecree = card("Zurgo, Thunder's Decree") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Orc Warrior"
    power = 2
    toughness = 4
    oracleText = "Mobilize 2 (Whenever this creature attacks, create two tapped and attacking 1/1 red " +
        "Warrior creature tokens. Sacrifice them at the beginning of the next end step.)\n" +
        "During your end step, Warrior tokens you control have \"This token can't be sacrificed.\""

    mobilize(2)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeSacrificed(
                filter = GroupFilter(GameObjectFilter.Token.youControl().withSubtype("Warrior"))
            ),
            condition = IsInStep(listOf(Step.END))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd93fb95-4268-45dc-8f0d-590c481a526d.jpg?1743204939"
    }
}
