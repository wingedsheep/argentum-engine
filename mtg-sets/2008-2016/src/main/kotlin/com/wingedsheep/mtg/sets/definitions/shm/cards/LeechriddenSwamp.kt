package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leechridden Swamp
 * Land — Swamp
 * ({T}: Add {B}.)
 * This land enters tapped.
 * {B}, {T}: Each opponent loses 1 life. Activate only if you control two or more black permanents.
 *
 * The {B} mana ability is intrinsic from the Swamp subtype. The drain is an ordinary activated
 * ability gated by [ActivationRestriction.OnlyIfCondition] over
 * [Conditions.YouControlAtLeast] with a black-permanent filter.
 */
val LeechriddenSwamp = card("Leechridden Swamp") {
    manaCost = ""
    typeLine = "Land — Swamp"
    oracleText = "({T}: Add {B}.)\n" +
        "This land enters tapped.\n" +
        "{B}, {T}: Each opponent loses 1 life. Activate only if you control two or more black permanents."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControlAtLeast(2, GameObjectFilter.Permanent.withColor(Color.BLACK))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "273"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afa47202-5824-4f6a-b306-465976d4d422.jpg?1783942706"
    }
}
