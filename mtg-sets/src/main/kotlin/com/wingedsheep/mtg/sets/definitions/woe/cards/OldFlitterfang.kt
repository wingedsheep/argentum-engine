package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Old Flitterfang
 * {4}{B}
 * Legendary Creature — Rat Faerie
 * 3/4
 *
 * Flying
 * At the beginning of each end step, if a creature died this turn, create a Food token.
 * {2}{B}, Sacrifice another creature or artifact: Old Flitterfang gets +2/+2 until end of turn.
 *
 * The Food trigger is an intervening-if (CR 603.4), which is exactly what the 2023-09-01 ruling
 * describes: the "a creature died this turn" check happens *as the end step begins*, and if nothing
 * died the ability never triggers at all — so [Conditions.CreatureDiedThisTurn] goes on
 * `triggerCondition`, not into a [com.wingedsheep.sdk.scripting.effects.ConditionalEffect] wrapper
 * around the effect. [Conditions.CreatureDiedThisTurn] is the global variant (any player's
 * creature), matching the unqualified "a creature".
 *
 * [Triggers.EachEndStep] rather than `YourEndStep`: it fires in every player's end step, so Old
 * Flitterfang converts an opponent's removal spell into a Food on their own turn.
 *
 * The pump's sacrifice is a *cost*, so it's paid on activation and can't be undone by removal in
 * response; [Costs.SacrificeAnother] over [GameObjectFilter.CreatureOrArtifact] enforces the printed
 * "another" (Old Flitterfang can't eat itself) while letting the Food tokens the trigger makes feed
 * it — Food is an artifact.
 */
val OldFlitterfang = card("Old Flitterfang") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Rat Faerie"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "At the beginning of each end step, if a creature died this turn, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{2}{B}, Sacrifice another creature or artifact: Old Flitterfang gets +2/+2 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EachEndStep
        triggerCondition = Conditions.CreatureDiedThisTurn
        effect = Effects.CreateFood()
        description = "At the beginning of each end step, if a creature died this turn, create a Food token."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        )
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "{2}{B}, Sacrifice another creature or artifact: Old Flitterfang gets " +
            "+2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "316"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67c77d6f-de14-423c-bf55-0fb289171004.jpg?1783915039"

        ruling(
            "2023-09-01",
            "Old Flitterfang's triggered ability will check as the end step starts to see if a " +
                "creature died this turn. If none did, the ability won't trigger at all."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a creature type."
        )
        ruling(
            "2024-11-08",
            "You can't sacrifice a Food to pay multiple costs. For example, you can't sacrifice a " +
                "Food token to activate its own ability and also to activate Maraleaf Rider's ability."
        )
    }
}
