package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aspect of Lamprey
 * {3}{B}
 * Enchantment — Aura
 * Enchant creature you control
 * When this Aura enters, target opponent discards two cards.
 * Enchanted creature has lifelink.
 *
 * Cartouche of Zeal's "enchant creature you control" frame with Strands of Undeath's enters
 * discard, aimed at a target opponent.
 */
val AspectOfLamprey = card("Aspect of Lamprey") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, target opponent discards two cards.\n" +
        "Enchanted creature has lifelink."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(count = 2, target = opponent)
        description = "When this Aura enters, target opponent discards two cards."
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Deruchenko Alexander"
        flavorText = "\"Some aspire to be mighty like the lion or the hydra. I am the parasite that feeds on them all.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27157f01-8aed-4485-9f0f-9e7486492e6e.jpg?1783931571"
        ruling(
            "2020-01-24",
            "If the creature Aspect of Lamprey would enchant is an illegal target by the time the Aura spell " +
                "resolves, the entire spell doesn't resolve. It won't enter the battlefield, so its ability won't trigger."
        )
    }
}
