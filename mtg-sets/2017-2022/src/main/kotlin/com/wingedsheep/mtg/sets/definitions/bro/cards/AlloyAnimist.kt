package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alloy Animist
 * {G}
 * Creature — Human Druid
 * 1/1
 *
 * {2}{G}: Until end of turn, target noncreature artifact you control becomes a 4/4 artifact creature.
 *
 * The animate adds the creature type and sets base P/T 4/4 while keeping the artifact's other
 * types and subtypes (per the 2022-10-14 ruling) — the same shape as Unctus's Retrofitter.
 */
val AlloyAnimist = card("Alloy Animist") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 1
    toughness = 1
    oracleText = "{2}{G}: Until end of turn, target noncreature artifact you control becomes a 4/4 artifact creature."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.notCreature().youControl()))
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 4,
            toughness = 4,
            addTypes = setOf("ARTIFACT"),
            duration = Duration.EndOfTurn,
        )
        description = "{2}{G}: Until end of turn, target noncreature artifact you control becomes a 4/4 artifact creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Kim Sokol"
        flavorText = "\"This metal belonged to Argoth before it was ripped from the ground. Now it belongs to Argoth again.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71a49b18-a339-401c-ab77-fcd04772cf69.jpg?1783920052"
        ruling("2022-10-14", "As Alloy Animist's ability resolves, the target permanent keeps any other types and subtypes it had before it became an artifact creature.")
    }
}
