package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valley Rally
 * {2}{R}
 * Instant
 *
 * Gift a Food (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a Food token before its other effects.)
 *
 * Creatures you control get +2/+0 until end of turn. If the gift was promised,
 * target creature you control gains first strike until end of turn.
 */
val ValleyRally = card("Valley Rally") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Gift a Food (You may promise an opponent a gift as you cast this spell. If you do, they create a Food token before its other effects. It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\nCreatures you control get +2/+0 until end of turn. If the gift was promised, target creature you control gains first strike until end of turn."

    val pumpAll = EffectPatterns.modifyStatsForAll(2, 0, Filters.Group.creaturesYouControl)

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — creatures you control get +2/+0 until end of turn
            Mode.noTarget(
                pumpAll,
                "Don't promise a gift — creatures you control get +2/+0 until end of turn"
            ),
            // Mode 2: Gift a Food — opponent creates Food, creatures get +2/+0, target creature gains first strike
            Mode.withTarget(
                Effects.CreateFood(1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(pumpAll)
                    .then(Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.ContextTarget(0))),
                Targets.CreatureYouControl,
                "Promise a gift — opponent creates a Food token, creatures you control get +2/+0 and target creature you control gains first strike until end of turn"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6178258-1ad6-4122-a56f-6eb7d0611e84.jpg?1721426740"
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
