package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Awaken the Sleeper — Phyrexia: All Will Be One #119 (only printing)
 * {3}{R} · Sorcery
 *
 * Act of Treason plus a rider: "If it's equipped, you may destroy all Equipment attached to that
 * creature." The "if" is a resolution-time state test on the target, so the yes/no prompt is only
 * offered when the creature actually has Equipment on it; the destroy gathers the attached
 * Equipment (Disarm's gather) and destroys each one.
 */
val AwakenTheSleeper = card("Awaken the Sleeper") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn. " +
        "If it's equipped, you may destroy all Equipment attached to that creature."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature.equipped(), t),
                Effects.May(
                    Effects.Pipeline(
                        descriptionOverride = "Destroy all Equipment attached to that creature."
                    ) {
                        val equipment = gather(
                            CardSource.AttachedTo(
                                host = t,
                                filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                            )
                        )
                        run(Effects.ForEachInCollection(equipment, Effects.Destroy(EffectTarget.IterationEntity)))
                    },
                    prompt = "Destroy all Equipment attached to that creature?",
                ),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Mathias Kollros"
        flavorText = "\"Without faith in each other, we truly would have nothing left. Isn't that right, Malach? ...Malach?\"\n—Darven, Mirran rebel, last words"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b92f866-2522-4f2a-a5ca-7d01ad79b927.jpg?1783918036"
    }
}
