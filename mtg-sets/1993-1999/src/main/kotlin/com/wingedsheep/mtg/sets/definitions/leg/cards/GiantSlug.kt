package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Giant Slug
 * {1}{B}
 * Creature — Slug
 * 1/1
 *
 * {5}: At the beginning of your next upkeep, choose a basic land type. This creature gains
 * landwalk of the chosen type until the end of that turn.
 *
 * The ability sets up a step-based delayed trigger on the controller's next upkeep (`NEXT_TURN`
 * timing, so activating during your own upkeep waits for the following one). The basic land type
 * is chosen as that trigger resolves — five labelled branches, each granting the matching
 * landwalk keyword until end of that turn.
 */
val GiantSlug = card("Giant Slug") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Slug"
    power = 1
    toughness = 1
    oracleText = "{5}: At the beginning of your next upkeep, choose a basic land type. This creature " +
        "gains landwalk of the chosen type until the end of that turn. (It can't be blocked as long " +
        "as defending player controls a land of that type.)"

    activatedAbility {
        cost = Costs.Mana("{5}")
        fun landwalk(landType: String, keyword: Keyword) = EffectChoice(
            label = landType,
            effect = Effects.GrantKeyword(keyword, EffectTarget.Self, Duration.EndOfTurn),
        )
        effect = Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            timing = DelayedTriggerTiming.NEXT_TURN,
            effect = Effects.ChooseAction(
                listOf(
                    landwalk("Plains", Keyword.PLAINSWALK),
                    landwalk("Island", Keyword.ISLANDWALK),
                    landwalk("Swamp", Keyword.SWAMPWALK),
                    landwalk("Mountain", Keyword.MOUNTAINWALK),
                    landwalk("Forest", Keyword.FORESTWALK),
                )
            ),
        )
        description = "{5}: At the beginning of your next upkeep, choose a basic land type. This " +
            "creature gains landwalk of the chosen type until the end of that turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43a612e5-a680-4c5b-8ce7-432a86240a6c.jpg?1783948066"
    }
}
