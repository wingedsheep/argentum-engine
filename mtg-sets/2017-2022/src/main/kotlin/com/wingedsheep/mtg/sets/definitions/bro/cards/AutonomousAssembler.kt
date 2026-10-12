package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Autonomous Assembler
 * {5}
 * Artifact Creature — Assembly-Worker
 * 4/5
 * Prototype {1}{W} — 2/2
 * Vigilance
 * {1}, {T}: Put a +1/+1 counter on target Assembly-Worker you control.
 *
 * "Assembly-Worker" is a subtype, so the target is any permanent with it (`Permanent`, not
 * `Creature`) — it may be the Assembler itself.
 */
val AutonomousAssembler = card("Autonomous Assembler") {
    manaCost = "{5}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 4
    toughness = 5
    oracleText = "Prototype {1}{W} — 2/2 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Vigilance\n" +
        "{1}, {T}: Put a +1/+1 counter on target Assembly-Worker you control."

    keywordAbility(KeywordAbility.prototype("{1}{W}", 2, 2))

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val worker = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.ASSEMBLY_WORKER).youControl()),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, worker)
        description = "{1}, {T}: Put a +1/+1 counter on target Assembly-Worker you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5dee863-e6ef-4061-a877-ddd3c4dbe87e.jpg?1783920118"
    }
}
