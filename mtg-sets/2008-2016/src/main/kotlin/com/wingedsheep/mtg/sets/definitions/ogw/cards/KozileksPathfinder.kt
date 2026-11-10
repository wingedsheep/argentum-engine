package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kozilek's Pathfinder
 * {6}
 * Creature — Eldrazi
 * 5/5
 * {C}: Target creature can't block this creature this turn. ({C} represents colorless mana.)
 *
 * The restriction is *pairwise* — the target is still free to block anything else — so this is
 * [Effects.CantBlock] with `attacker = EffectTarget.Self`, the same shape as Screeching Griffin.
 */
val KozileksPathfinder = card("Kozilek's Pathfinder") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 5
    oracleText = "{C}: Target creature can't block this creature this turn. ({C} represents colorless mana.)"

    activatedAbility {
        cost = Costs.Mana("{C}")
        val t = target(TargetFilter.Creature)
        effect = Effects.CantBlock(target = t, attacker = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "James Zapata"
        flavorText = "\"We didn't know where it came from or where it was going. All we knew was that we did not matter.\"\n—Akiri, kor line-slinger"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b75585bd-fdd8-44d1-8c7c-b96df5fc473e.jpg?1783937930"
        ruling("2016-01-22", "Activating the ability once Kozilek's Pathfinder has been legally blocked won't change or undo that block.")
    }
}
