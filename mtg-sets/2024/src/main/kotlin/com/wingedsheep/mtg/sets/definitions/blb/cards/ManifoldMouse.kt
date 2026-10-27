package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Step

/**
 * Manifold Mouse
 * {1}{R}
 * Creature — Mouse Soldier
 * 1/2
 *
 * Offspring {2} (You may pay an additional {2} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 *
 * At the beginning of combat on your turn, target Mouse you control gains your
 * choice of double strike or trample until end of turn.
 */
val ManifoldMouse = card("Manifold Mouse") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Mouse Soldier"
    power = 1
    toughness = 2
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nAt the beginning of combat on your turn, target Mouse you control gains your choice of double strike or trample until end of turn."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.offspring("{2}"))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // At the beginning of combat on your turn, target Mouse you control gains
    // your choice of double strike or trample until end of turn.
    triggeredAbility {
        val creature = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Mouse").youControl()))
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature, Duration.EndOfTurn),
                "Double strike"
            ),
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.TRAMPLE, creature, Duration.EndOfTurn),
                "Trample"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db3832b5-e83f-4569-bd49-fb7b86fa2d47.jpg?1721426663"
    }
}
